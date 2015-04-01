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
import java.util.List;

import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticBean;
import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticService;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumEventListener;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.ForumStatistic;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * 4 Apr 2014  
 */
public class PlatformForumEventListener extends ForumEventListener {

  private  ActivityStatisticService activityStatsService;
  private  ForumService forumService ;
  private static Log log_ = ExoLogger.getLogger(PlatformForumEventListener.class);
  private final long DAY_IN_MILLISEC = 86400000;
  
  public PlatformForumEventListener(ActivityStatisticService activityStatsService, ForumService forumService){
    this.activityStatsService = activityStatsService;
    this.forumService = forumService;
  }
  
  private void updateActivityStatistic() {
    try {
     
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
  
  @Override
  public void addTopic(Topic topic) {
    updateActivityStatistic();  
  }

  @Override
  public void updateTopic(Topic topic) {
    // do nothing
    
  }

  @Override
  public void updateTopics(List<Topic> topics, boolean isLock) {
 // do nothing
    
  }

  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {
    updateActivityStatistic();
  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {
    updateActivityStatistic();
  }

  @Override
  public void addPost(Post post) {
    updateActivityStatistic();
  }

  @Override
  public void updatePost(Post post) {
    // do nothing
  }

  @Override
  public void updatePost(Post post, int type) {
    // do nothing
  }

  @Override
  public void removeActivity(String activityId) {
    updateActivityStatistic();
  }

  @Override
  public void removeComment(String activityId, String commentId) {
    updateActivityStatistic();
  }

  @Override
  public void saveCategory(Category category) {
    updateActivityStatistic();
  }

  @Override
  public void saveForum(Forum forum) {
    updateActivityStatistic();
  }

  @Override
  public void movePost(List<Post> posts, List<String> srcPostActivityIds, String desTopicPath) {
    // TODO Auto-generated method stub
    
  }

}
