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
package org.exoplatform.addon.pulse.service.ws;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * 6 Jun 2014  
 */
public class ChartData {
  private List<String> listTitle;
  private List<Long> newUsersData;
  private List<Long> loginCountData;
  private List<Long> forumActiveUsersData;
  private List<Long> newForumPostsData;
  private List<Long> plfDownloadsData;
  private List<Long> userConnectionData;
  private List<Long> socialPostData;
  private List<Long> emailNotificationData;
  
  public List<String> getListTitle() {
    return listTitle;
  }
  public void setListTitle(List<String> listTitle) {
    this.listTitle = listTitle;
  }
  public List<Long> getNewUsersData() {
    return newUsersData;
  }
  public void setNewUsersData(List<Long> newUsersData) {
    this.newUsersData = newUsersData;
  }
  public List<Long> getLoginCountData() {
    return loginCountData;
  }
  public void setLoginCountData(List<Long> loginCountData) {
    this.loginCountData = loginCountData;
  }
  public List<Long> getForumActiveUsersData() {
    return forumActiveUsersData;
  }
  public void setForumActiveUsersData(List<Long> forumActiveUsersData) {
    this.forumActiveUsersData = forumActiveUsersData;
  }
  public List<Long> getNewForumPostsData() {
    return newForumPostsData;
  }
  public void setNewForumPostsData(List<Long> newForumPostsData) {
    this.newForumPostsData = newForumPostsData;
  }
  public List<Long> getPlfDownloadsData() {
    return plfDownloadsData;
  }
  public void setPlfDownloadsData(List<Long> plfDownloadsData) {
    this.plfDownloadsData = plfDownloadsData;
  }
  public List<Long> getUserConnectionData() {
    return userConnectionData;
  }
  public void setUserConnectionData(List<Long> userConnectionData) {
    this.userConnectionData = userConnectionData;
  }
  public List<Long> getSocialPostData() {
    return socialPostData;
  }
  public void setSocialPostData(List<Long> socialPostData) {
    this.socialPostData = socialPostData;
  }
  public List<Long> getEmailNotificationData() {
    return emailNotificationData;
  }
  public void setEmailNotificationData(List<Long> emailNotificationData) {
    this.emailNotificationData = emailNotificationData;
  }
  
  

}
