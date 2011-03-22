/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */

package org.switchyard.internal;

import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.switchyard.Context;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.ExchangePhase;
import org.switchyard.ExchangeState;
import org.switchyard.Message;
import org.switchyard.ServiceReference;
import org.switchyard.handlers.HandlerChain;
import org.switchyard.metadata.ExchangeContract;
import org.switchyard.spi.Dispatcher;
import org.switchyard.transform.TransformSequence;

/**
 * Implementation of Exchange.
 */
public class ExchangeImpl implements Exchange {

    private static Logger _log = Logger.getLogger(ExchangeImpl.class);

    private final String            _exchangeId;
    private final ExchangeContract  _contract;
    private ExchangePhase           _phase;
    private final ServiceReference  _service;
    private Message                 _message;
    private ExchangeState           _state = ExchangeState.OK;
    private Dispatcher              _dispatch;
    private HandlerChain            _replyChain;
    private final Context           _context;

    /**
     * Create a new exchange with no endpoints initialized.  At a minimum, the 
     * input endpoint must be set before sending an exchange.
     * @param service service
     * @param contract exchange contract
     * @param dispatch dispatcher to use for the exchange
     */
    public ExchangeImpl(ServiceReference service, ExchangeContract contract, Dispatcher dispatch) {
        this(service, contract, dispatch, null);
    }
    
    /**
     * Constructor.
     * @param service service
     * @param contract exchange contract
     * @param input input endpoint
     * @param input output endpoint
     */
    public ExchangeImpl(ServiceReference service, ExchangeContract contract, Dispatcher dispatch, HandlerChain replyChain) {

        // Check that the ExchangeContract exists and has invoker metadata and a ServiceOperation defined on it...
        if (contract == null) {
            throw new IllegalArgumentException("null 'contract' arg.");
        } else if (contract.getInvokerInvocationMetaData() == null) {
            throw new IllegalArgumentException("Invalid 'contract' arg.  No invoker invocation metadata defined on the contract instance.");
        } else if (contract.getServiceOperation() == null) {
            throw new IllegalArgumentException("Invalid 'contract' arg.  No ServiceOperation defined on the contract instance.");
        }

        // Make sure we have an output endpoint when the pattern is IN_OUT...
        ExchangePattern exchangePattern = contract.getServiceOperation().getExchangePattern();
        if (replyChain == null && exchangePattern == ExchangePattern.IN_OUT) {
            throw new RuntimeException("Invalid Exchange construct.  Must supply an reply handler for an IN_OUT Exchange.");
        }

        _service = service;
        _contract = contract;
        _dispatch = dispatch;
        _replyChain = replyChain;
        _exchangeId = UUID.randomUUID().toString();
        _context = new DefaultContext();
    }
    
    /**
     * Creates an exchange implementation in a specific state.  This constructor is
     * used when deserializing an exchange.
     * @param exchangeId exchange unique ID
     * @param dispatch dispatcher used to send exchange
     * @param phase exchange phase
     */
    public ExchangeImpl(String exchangeId, Dispatcher dispatch, ExchangePhase phase, ExchangeContract contract) {
        _exchangeId = exchangeId;
        _dispatch = dispatch;
        _phase = phase;
        _contract = contract;
        
        // TODO : update once serialization impl is ready
        _service = null;
        _context = null;
    }

    @Override
    public Context getContext() {
        return _context;
    }

    @Override
    public ExchangeContract getContract() {
        return _contract;
    }

    @Override
    public ServiceReference getService() {
        return _service;
    }

    @Override
    public String getId() {
        return _exchangeId;
    }

    @Override
    public Message getMessage() {
        return _message;
    }

    @Override
    public void send(Message message) {
        assertExchangeStateOK();
        
        // Set exchange phase
        if (_phase == null) {
            _phase = ExchangePhase.IN;
            initInTransformSequence(message);
        } else if (_phase.equals(ExchangePhase.IN)) {
            _phase = ExchangePhase.OUT;
            initOutTransformSequence(message);
        } else {
            throw new IllegalStateException(
                    "Send message not allowed for exchange in phase " + _phase);
        }
        
        sendInternal(message);
    }

    @Override
    public void sendFault(Message message) {
        assertExchangeStateOK();
        
        // You can't send a fault before you send a message
        if (_phase == null) {
            throw new IllegalStateException("Send fault no allowed on new exchanges");        
        }
        
        _phase = ExchangePhase.OUT;
        _state = ExchangeState.FAULT;
        sendInternal(message);
    }

    @Override
    public ExchangeState getState() {
        return _state;
    }

    /**
     * Get exchange dispatcher.
     * @return exchange dispatcher
     */
    public Dispatcher getDispatcher() {
        return _dispatch;
    }
    
    /**
     * Get the reply handler chain for this exchange.
     * @return reply chain
     */
    public HandlerChain getReplyChain() {
        return _replyChain;
    }

    /**
     * Set the exchange dispatcher.
     * @param dispatch exchange dispatcher
     */
    public void setOutputDispatcher(Dispatcher dispatch) {
        _dispatch = dispatch;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExchangeImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        
        // two exchanges with the same id are equal
        return ((ExchangeImpl)obj).getId().equals(_exchangeId);
    }
    
    @Override
    public int hashCode() {
        return _exchangeId.hashCode();
    }

    /**
     * Internal send method common to sendFault and sendMessage.  This method
     * assumes that the exchange phase has been assigned for the send and that
     * the exchange state has been verified (i.e. it's OK to deliver in the
     * exchange's current state).
     */
    private void sendInternal(Message message) {
        _message = message;
        // if a fault was thrown by the handler chain and there's no reply chain
        // we need to log.
        // TODO : stick this in a central fault/error queue
        if (ExchangeState.FAULT.equals(_state) && _replyChain == null) {
            _log.warn("Fault generated during exchange without a handler: " + _message);
            return;
        }
        _dispatch.dispatch(this);
    }

    private void assertExchangeStateOK() {
        if (_state == ExchangeState.FAULT) {
            throw new IllegalStateException("Exchange instance is in a FAULT state.");
        }
    }

    @Override
    public Message createMessage() {
        return new DefaultMessage();
    }

    @Override
    public ExchangePhase getPhase() {
        return _phase;
    }

    private void initInTransformSequence(Message message) {
        QName exchangeInputType = _contract.getInvokerInvocationMetaData().getInputType();
        QName serviceOperationInputType = _contract.getServiceOperation().getInputType();

        if (exchangeInputType != null && serviceOperationInputType != null) {
            TransformSequence.
                    from(exchangeInputType).
                    to(serviceOperationInputType).
                    associateWith(message.getContext());
        }
    }

    private void initOutTransformSequence(Message message) {
        QName serviceOperationOutputType = _contract.getServiceOperation().getOutputType();
        QName exchangeOutputType = _contract.getInvokerInvocationMetaData().getOutputType();

        if (serviceOperationOutputType != null && exchangeOutputType != null) {
            TransformSequence.
                    from(serviceOperationOutputType).
                    to(exchangeOutputType).
                    associateWith(message.getContext());
        }
    }
}
