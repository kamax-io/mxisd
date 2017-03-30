/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.lookup.strategy

import edazdarevic.commons.net.CIDRUtils
import io.kamax.mxisd.api.ThreePidType
import io.kamax.mxisd.config.RecursiveLookupConfig
import io.kamax.mxisd.lookup.LookupRequest
import io.kamax.mxisd.lookup.provider.ThreePidProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RecursivePriorityLookupStrategy implements LookupStrategy, InitializingBean {

    private Logger log = LoggerFactory.getLogger(RecursivePriorityLookupStrategy.class)

    @Autowired
    private RecursiveLookupConfig recursiveCfg

    @Autowired
    private List<ThreePidProvider> providers

    private List<CIDRUtils> allowedCidr = new ArrayList<>()

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Found ${providers.size()} providers")

        providers.sort(new Comparator<ThreePidProvider>() {

            @Override
            int compare(ThreePidProvider o1, ThreePidProvider o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority())
            }

        })

        log.info("Recursive lookup enabled: {}", recursiveCfg.isEnabled())
        for (String cidr : recursiveCfg.getAllowedCidr()) {
            log.info("{} is allowed for recursion", cidr)
            allowedCidr.add(new CIDRUtils(cidr))
        }
    }

    @Override
    Optional<?> find(LookupRequest request) {
        if (ThreePidType.email != request.getType()) {
            /* throw new IllegalArgumentException("${request.getType()} is currently not supported") */
            /* Not to break bulk_lookup */
            return Optional.empty()
        }

        boolean canRecurse = false
        if (recursiveCfg.isEnabled()) {
            log.debug("Checking {} CIDRs for recursion", allowedCidr.size())
            for (CIDRUtils cidr : allowedCidr) {
                if (cidr.isInRange(request.getRequester())) {
                    log.debug("{} is in range {}, allowing recursion", request.getRequester(), cidr.getNetworkAddress())
                    canRecurse = true
                    break
                } else {
                    log.debug("{} is not in range {}", request.getRequester(), cidr.getNetworkAddress())
                }
            }
        }
        log.info("Host {} allowed for recursion: {}", request.getRequester(), canRecurse)

        for (ThreePidProvider provider : providers) {
            if (provider.isLocal() || canRecurse) {
                Optional<?> lookupDataOpt = provider.find(request)
                if (lookupDataOpt.isPresent()) {
                    return lookupDataOpt
                }
            }
        }

        return Optional.empty()
    }

}
