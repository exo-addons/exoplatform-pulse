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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;

/**
 * Created by The eXo Platform SAS
 * 4 Apr 2014  
 */
public class SocialActivitytListener extends ActivityListenerPlugin {

  private  ActivityStatisticService activityStatsService;
  private static Log log_ = ExoLogger.getLogger(SocialActivitytListener.class);
  
  public SocialActivitytListener(ActivityStatisticService activityStatsService){
    this.activityStatsService = activityStatsService;
  }
  
  private void updateActivityStatistic(){
    try {
      long now = System.currentTimeMillis();
      
      ActivityStatisticBean todayStats = activityStatsService.getActivityStatisticByDate(new Date(now));
      ActivityStatisticBean objectToUpdate =  new ActivityStatisticBean(); 
      
      if(null == todayStats){
        todayStats = activityStatsService.startTodayStatistic();  
      }
  
      objectToUpdate.setCreatedDate(todayStats.getCreatedDate());
      objectToUpdate.setModifiedDate(new Date());
      objectToUpdate.setSocialPostCountToday(todayStats.getSocialPostCountToday() + 1);      
  
      activityStatsService.createOrUpdate(objectToUpdate);
    } catch (Exception e) {
      log_.error(e);
    }
  }
  @Override
  public void saveActivity(ActivityLifeCycleEvent event) {
    updateActivityStatistic();
  }

  @Override
  public void updateActivity(ActivityLifeCycleEvent event) {
    // do Nothing
    
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent event) {
    // do Nothing
    
  }

  @Override
  public void likeActivity(ActivityLifeCycleEvent event) {
    // do Nothing
    
  }
  

}
