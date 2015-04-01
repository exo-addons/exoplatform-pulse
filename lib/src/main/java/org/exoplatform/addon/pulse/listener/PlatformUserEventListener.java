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
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

/**
 * Created by The eXo Platform SAS
 * Mar 27, 2014  
 */
public class PlatformUserEventListener extends UserEventListener {
  private static final Log LOG = ExoLogger.getLogger(PlatformUserEventListener.class);
  private final ActivityStatisticService activityStatsService;
  private final long DAY_IN_MILLISEC=86400000;
  
  public PlatformUserEventListener(ActivityStatisticService activityStatsService){
    this.activityStatsService = activityStatsService;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.services.organization.UserEventListener#preSave(org.exoplatform.services.organization.User, boolean)
   */
  @Override
  public void preSave(User user, boolean isNew) throws Exception {
    // TODO Auto-generated method stub
    super.preSave(user, isNew);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.organization.UserEventListener#postSave(org.exoplatform.services.organization.User, boolean)
   */
  @Override
  public void postSave(User user, boolean isNew) throws Exception {
    // TODO Auto-generated method stub
    
    try {
      if(isNew){
        long now = System.currentTimeMillis();
        
        ActivityStatisticBean todayStats = activityStatsService.getActivityStatisticByDate(new Date(now));
        ActivityStatisticBean objectToUpdate =  new ActivityStatisticBean(); 
        
        if(null == todayStats){
          todayStats = activityStatsService.startTodayStatistic();  
        }
       
        objectToUpdate.setCreatedDate(todayStats.getCreatedDate());
        objectToUpdate.setModifiedDate(new Date());
        objectToUpdate.setNewUserToday(todayStats.getNewUserToday() + 1);        
       
        activityStatsService.createOrUpdate(objectToUpdate);

      }
    } catch (Exception e) {
      LOG.debug("Error while counting the new registration: " + e.getMessage(), e);
    }
    super.postSave(user, isNew);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.organization.UserEventListener#preDelete(org.exoplatform.services.organization.User)
   */
  @Override
  public void preDelete(User user) throws Exception {
    // TODO Auto-generated method stub
    super.preDelete(user);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.organization.UserEventListener#postDelete(org.exoplatform.services.organization.User)
   */
  @Override
  public void postDelete(User user) throws Exception {
    // TODO Auto-generated method stub
    super.postDelete(user);
  }

}
