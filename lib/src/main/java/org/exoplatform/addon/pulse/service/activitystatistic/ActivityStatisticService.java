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
import java.util.List;


/**
 * Created by The eXo Platform SAS
 * Mar 25, 2014  
 */
public interface ActivityStatisticService {
  
  public static final String HOME = "exo:activityStatisticHome";
  public static final String ACTIVITY_HOME_NOTE_TYPE = "exo:activityStatisticHome";
  public static final String ACTIVITY_STATISTIC_NOTE_TYPE = "exo:activityStatistic";
  public static final String ACTIVITY_STATISTIC_BY_DAY_NOTE_TYPE = "exo:activityStatisticByDay";
  
  public static final String ACTIVITY_STATISTIC_CREATED_DATE = "exo:createdDate";
  public static final String ACTIVITY_STATISTIC_MODIFIED_DATE = "exo:modifiedDate";
  public static final String ACTIVITY_STATISTIC_NEW_USER = "exo:newUser";
  public static final String ACTIVITY_STATISTIC_NEW_USER_TODAY = "exo:newUserToday";
  public static final String ACTIVITY_STATISTIC_FORUM_ACTIVE_USER_TODAY = "exo:forumActiveUserToday";
  public static final String ACTIVITY_STATISTIC_FORUM_POST = "exo:forumPosts";
  public static final String ACTIVITY_STATISTIC_FORUM_POST_TODAY = "exo:forumPostToday";
  public static final String ACTIVITY_STATISTIC_LOGIN_COUNT = "exo:loginCount";
  public static final String ACTIVITY_STATISTIC_LOGIN_COUNT_TODAY = "exo:loginCountToday";
  public static final String ACTIVITY_STATISTIC_USER_CONNECTION_COUNT = "exo:userConnectionCount";
  public static final String ACTIVITY_STATISTIC_USER_CONNECTION_COUNT_TODAY = "exo:userConnectionCountToday";
  public static final String ACTIVITY_STATISTIC_SOCIAL_POST_COUNT = "exo:socialPostCount";
  public static final String ACTIVITY_STATISTIC_SOCIAL_POST_COUNT_TODAY = "exo:socialPostCountToday";
  public static final String ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT = "exo:emailNotificationCount";
  public static final String ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT_TODAY = "exo:emailNotificationToday";
  
  public abstract boolean createOrUpdate (ActivityStatisticBean bean) throws Exception;

  public abstract ActivityStatisticBean getActivityStatisticByDate (Date date) throws Exception;
  
  public abstract ActivityStatisticBean startTodayStatistic () throws Exception;

  public abstract List<ActivityStatisticBean> getListActivityStatisticByDate (Date fromDate, Date toDate) throws Exception;
  
  public abstract boolean upgrade() throws Exception;
}
