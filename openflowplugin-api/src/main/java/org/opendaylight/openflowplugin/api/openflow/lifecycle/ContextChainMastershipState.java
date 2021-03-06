/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.api.openflow.lifecycle;

public enum ContextChainMastershipState {
    /**
     * Device has role MASTER set.
     */
    MASTER_ON_DEVICE,
    /**
     * Initial submit ok.
     */
    INITIAL_SUBMIT,
    /**
     * Registration of RPC services is done.
     */
    RPC_REGISTRATION,
    /**
     * Check mastership.
     */
    CHECK
}
