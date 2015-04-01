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
/**
 * Created by The eXo Platform SAS
 * Mar 25, 2014  
 */
public class ActivityStatisticBean {


    private Date createdDate; 
    
    private Date modifiedDate; 

    private Long newUser;// cumulative count of new user yesterday
    
    private Long newUserToday; // count of new user today

    private Long forumActiveUserToday; // number of active user today
    
    private Long forumPosts;// cumulative count of forum posts yesterday
    
    private Long forumPostToday;// count of forum posts today
    
    private Long loginCount;// cumulative count of users login yesterday
    
    private Long loginCountToday;// count of users login today
    
    
    private Long userConnectionCount;// cumulative count of users connection yesterday
    
    private Long userConnectionCountToday;// count of users connections today

    private Long socialPostCount;// cumulative count of social posts (social activities) yesterday
    
    private Long socialPostCountToday;// count count of social posts (social activities) today

    private Long emailNotificationCount;// cumulative count of notification emails yesterday
    
    private Long emailNotificationCountToday;// count of notification emails today



    public Date getCreatedDate() {
      return createdDate;
    }


    public void setCreatedDate(Date createdDate) {
      this.createdDate = createdDate;
    }


    public Long getNewUser() {
      return newUser;
    }


    public void setNewUser(Long newUser) {
      this.newUser = newUser;
    }


    public Long getNewUserToday() {
      return newUserToday;
    }


    public void setNewUserToday(Long newUserToday) {
      this.newUserToday = newUserToday;
    }


    public Long getForumActiveUserToday() {
      return forumActiveUserToday;
    }


    public void setForumActiveUserToday(Long forumActiveUserToday) {
      this.forumActiveUserToday = forumActiveUserToday;
    }


    public Long getForumPosts() {
      return forumPosts;
    }


    public void setForumPosts(Long forumPosts) {
      this.forumPosts = forumPosts;
    }


    public Long getForumPostToday() {
      return forumPostToday;
    }


    public void setForumPostToday(Long forumPostToday) {
      this.forumPostToday = forumPostToday;
    }


    public Long getLoginCount() {
      return loginCount;
    }


    public void setLoginCount(Long loginCount) {
      this.loginCount = loginCount;
    }


    public Long getLoginCountToday() {
      return loginCountToday;
    }


    public void setLoginCountToday(Long loginCountToday) {
      this.loginCountToday = loginCountToday;
    }


    public Date getModifiedDate() {
      return modifiedDate;
    }


    public void setModifiedDate(Date modifiedDate) {
      this.modifiedDate = modifiedDate;
    }


    public Long getUserConnectionCount() {
      return userConnectionCount;
    }


    public void setUserConnectionCount(Long userConnectionCount) {
      this.userConnectionCount = userConnectionCount;
    }


    public Long getUserConnectionCountToday() {
      return userConnectionCountToday;
    }


    public void setUserConnectionCountToday(Long userConnectionCountToday) {
      this.userConnectionCountToday = userConnectionCountToday;
    }


    public Long getSocialPostCount() {
      return socialPostCount;
    }


    public void setSocialPostCount(Long socialPostCount) {
      this.socialPostCount = socialPostCount;
    }


    public Long getSocialPostCountToday() {
      return socialPostCountToday;
    }


    public void setSocialPostCountToday(Long socialPostCountToday) {
      this.socialPostCountToday = socialPostCountToday;
    }


    public Long getEmailNotificationCount() {
      return emailNotificationCount;
    }


    public void setEmailNotificationCount(Long emailNotificationCount) {
      this.emailNotificationCount = emailNotificationCount;
    }


    public Long getEmailNotificationCountToday() {
      return emailNotificationCountToday;
    }


    public void setEmailNotificationCountToday(Long emailNotificationCountToday) {
      this.emailNotificationCountToday = emailNotificationCountToday;
    }

  }

