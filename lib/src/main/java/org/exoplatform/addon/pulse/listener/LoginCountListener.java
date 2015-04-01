/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.pulse.listener;

import java.util.Date;

import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticBean;
import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS
 * Mar 28, 2014  
 */
public class LoginCountListener extends Listener<ConversationRegistry, ConversationState> {

  private static final Log LOG = ExoLogger.getLogger(LoginCountListener.class);
  private final ActivityStatisticService activityStatsService;  

  public LoginCountListener(ActivityStatisticService activityStatsService) throws Exception {
      this.activityStatsService = activityStatsService;
  }
 
  
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    String userId = event.getData().getIdentity().getUserId();
    try {
      long now = System.currentTimeMillis();
      
      ActivityStatisticBean todayStats = activityStatsService.getActivityStatisticByDate(new Date(now));
      ActivityStatisticBean objectToUpdate =  new ActivityStatisticBean(); 
      
      if(null == todayStats){
        todayStats = activityStatsService.startTodayStatistic();
      }
     
      objectToUpdate.setCreatedDate(todayStats.getCreatedDate());
      objectToUpdate.setModifiedDate(new Date());
      objectToUpdate.setLoginCountToday(todayStats.getLoginCountToday() + 1);        
     
      activityStatsService.createOrUpdate(objectToUpdate);
    } catch (Exception e) {
      LOG.debug("Error while counting the login of user '" + userId + "': " + e.getMessage(), e);
    }
    
  }

}
