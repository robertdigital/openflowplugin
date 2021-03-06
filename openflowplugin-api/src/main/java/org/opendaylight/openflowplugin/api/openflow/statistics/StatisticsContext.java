/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.api.openflow.statistics;

import org.opendaylight.openflowplugin.api.openflow.OFPContext;
import org.opendaylight.openflowplugin.api.openflow.device.RequestContextStack;
import org.opendaylight.openflowplugin.api.openflow.lifecycle.DeviceInitializationContext;
import org.opendaylight.openflowplugin.api.openflow.lifecycle.ReconciliationFrameworkStep;

/**
 * Context for statistics.
 */
public interface StatisticsContext extends RequestContextStack, OFPContext, ReconciliationFrameworkStep,
        DeviceInitializationContext {
    /**
     * Enables statistics gathering.
     */
    void enableGathering();

    /**
     * Disables statistics gathering.
     */
    void disableGathering();
}