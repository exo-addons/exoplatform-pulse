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
package org.exoplatform.addon.pulse.service.activitystatistic;

import java.util.Date;

import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.ForumStatistic;
import org.exoplatform.job.MultiTenancyJob;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.JobExecutionContext;



/**
 * Created by The eXo Platform SAS
 * Mar 31, 2014  
 */
public class ForumStatisticCounterJob extends MultiTenancyJob {
  
  private static Log log_ = ExoLogger.getLogger("job.forum.RecountActiveUserJob");//change class name
  
  private final long DAY_IN_MILLISEC = 86400000;

  @Override
  public Class<? extends MultiTenancyTask> getTask() {

    return ForumCounterTask.class;
  }
  

  public class ForumCounterTask extends MultiTenancyTask {
    
    public ForumCounterTask(JobExecutionContext context, String repoName) {
      super(context, repoName);
    }
  
    @Override
    public void run() {
      super.run();
      try {
        ForumService forumService = (ForumService) container.getComponentInstanceOfType(ForumService.class);
        ActivityStatisticService activityStatsService = (ActivityStatisticService) container.getComponentInstanceOfType(ActivityStatisticService.class);
        if (forumService != null && activityStatsService != null) {
          long now = System.currentTimeMillis();
          ForumStatistic forumStats = forumService.getForumStatistic();
          
          ActivityStatisticBean todayStats = activityStatsService.getActivityStatisticByDate(new Date(now));
          ActivityStatisticBean objectToUpdate =  new ActivityStatisticBean(); 
          
          if(null == todayStats){
            todayStats = activityStatsService.startTodayStatistic();
            todayStats.setForumPosts(forumStats.getPostCount());
          }
          
          objectToUpdate.setCreatedDate(todayStats.getCreatedDate());
          objectToUpdate.setModifiedDate(new Date());
          
          objectToUpdate.setForumPostToday(forumStats.getPostCount() - todayStats.getForumPosts());
          
          ActivityStatisticBean yesterdayStats = activityStatsService.getActivityStatisticByDate(new Date(now - DAY_IN_MILLISEC));
          long yestPost = todayStats.getForumPosts();
          if(null != yesterdayStats){
           yestPost = yesterdayStats.getForumPosts() + yesterdayStats.getForumPostToday();
          }
          objectToUpdate.setForumPosts(yestPost);
          objectToUpdate.setForumActiveUserToday(forumStats.getActiveUsers());
          activityStatsService.createOrUpdate(objectToUpdate);
        }
      }catch (Exception e) {
        if (log_.isDebugEnabled()) {
          log_.debug("Can not update Activity Statustic: " + e.getMessage());
        }
      }
    }
  }
}

