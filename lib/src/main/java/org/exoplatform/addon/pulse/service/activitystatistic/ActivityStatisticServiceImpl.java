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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;

/**
 * Created by The eXo Platform SAS Mar 25, 2014
 */
public class ActivityStatisticServiceImpl implements ActivityStatisticService {

  private static final SimpleDateFormat formatDateTime = new SimpleDateFormat();
  private static final String TIME_FORMAT_TAIL_TO = "T23:59:59.999";
  private static final String TIME_FORMAT_TAIL_FROM = "T00:00:00.000";
  private final long DAY_IN_MILLISEC=86400000;
  private RepositoryService repositoryService;

  private static final Log  LOG = ExoLogger.getLogger(ActivityStatisticServiceImpl.class);

  static {
    formatDateTime.applyPattern("yyyy-MM-dd");
  }
  
  public ActivityStatisticServiceImpl(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Deprecated
  protected void createHomeNode() throws RepositoryException {
    SessionProvider sessionProvider = null;

    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
      Node rootNode = session.getRootNode();
      if (!rootNode.hasNode(HOME)) {
        Node homeNode = rootNode.addNode(HOME, "exo:activityStatisticHome");
        rootNode.save();
        //Hide home node
        if (homeNode.canAddMixin("exo:hiddenable")) {
          homeNode.addMixin("exo:hiddenable");
        }

        LOG.info("ActivityStatisticHome is initialized.");
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  @Override
  public boolean createOrUpdate(ActivityStatisticBean bean) throws Exception {
    Date createdDate = bean.getCreatedDate();
    if(null == createdDate)
      return false;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(createdDate);
    String date = String.valueOf(calendar.get(Calendar.DATE));
    String month = String.valueOf(calendar.get(Calendar.MONTH) +1);
    String year = String.valueOf(calendar.get(Calendar.YEAR));

    String nodePath = HOME + "/" + year + "/" + month + "/" + date;

    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
      Node rootNode = session.getRootNode();
      if (!rootNode.hasNode(nodePath)) {
        return createActivityStatistic(bean, year, month, date);
      }else{
        return updateActivityStatistic(bean, nodePath);
      }
    }catch (Exception e){
      LOG.error(e);
      return false;
    }finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Create the nodes in tree structure yyyy/MM/dd  
   * @param bean
   * @return
   */
  private boolean createActivityStatistic(ActivityStatisticBean bean, String year, String month, String date) throws Exception {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      //Create home node if it is not existed
      ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
      Node rootNode = session.getRootNode();
      Node homeNode = null;
      if (!rootNode.hasNode(HOME)) {
        homeNode = rootNode.addNode(HOME, "exo:activityStatisticHome");
        rootNode.save();
        //Hide home node
        if (homeNode.canAddMixin("exo:hiddenable")) {
          homeNode.addMixin("exo:hiddenable");
          homeNode.save();
        }

        LOG.info("ActivityStatisticHome is initialized.");
      }else{
        homeNode = rootNode.getNode(HOME);
      }
       
      Node yearNode = null;
      Node monthNode = null;
      Node dateNode = null;
      if (!homeNode.hasNode(year)) {
        yearNode = homeNode.addNode(year, ACTIVITY_STATISTIC_NOTE_TYPE);
        homeNode.save();
      }else{
        yearNode = homeNode.getNode(year);
      }
      if(!yearNode.hasNode(month)){
        monthNode = yearNode.addNode(month, ACTIVITY_STATISTIC_NOTE_TYPE);
        yearNode.save();
      }else{
        monthNode = yearNode.getNode(month);
      }
        
      if(!monthNode.hasNode(date)){
        dateNode = monthNode.addNode(date, ACTIVITY_STATISTIC_BY_DAY_NOTE_TYPE);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date() );
        dateNode.setProperty(ACTIVITY_STATISTIC_CREATED_DATE, cal);
        
        if(bean.getNewUser() != null && bean.getNewUser() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_NEW_USER, bean.getNewUser());
        }
        
        if(bean.getNewUserToday() != null && bean.getNewUserToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_NEW_USER_TODAY, bean.getNewUserToday());
        }
        
        if(bean.getForumActiveUserToday() != null && bean.getForumActiveUserToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_ACTIVE_USER_TODAY, bean.getForumActiveUserToday());
        }
        
        if(bean.getForumPosts() != null && bean.getForumPosts() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_POST, bean.getForumPosts());
        }
        
        if(bean.getForumPostToday() != null && bean.getForumPostToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_POST_TODAY, bean.getForumPostToday());
        }
        
        if(bean.getLoginCount() != null && bean.getLoginCount() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_LOGIN_COUNT, bean.getLoginCount());
        }
        
        if(bean.getLoginCountToday() != null && bean.getLoginCountToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_LOGIN_COUNT_TODAY, bean.getLoginCountToday());
        }
        
        
        if(bean.getUserConnectionCount() != null && bean.getUserConnectionCount() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT, bean.getUserConnectionCount());
        }
        if(bean.getUserConnectionCountToday() != null && bean.getUserConnectionCountToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT_TODAY, bean.getUserConnectionCountToday());
        }
        
        if(bean.getSocialPostCount() != null && bean.getSocialPostCount() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT, bean.getSocialPostCount());
        }
        if(bean.getSocialPostCountToday() != null && bean.getSocialPostCountToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT_TODAY, bean.getSocialPostCountToday());
        }
        
        if(bean.getEmailNotificationCount() != null && bean.getEmailNotificationCount() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT, bean.getEmailNotificationCount());
        }
        if(bean.getEmailNotificationCountToday() != null && bean.getEmailNotificationCountToday() >= 0){
          dateNode.setProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT_TODAY, bean.getEmailNotificationCountToday());
        }
        
        monthNode.save();
      }
      return true;
      
    }catch (Exception e){
      LOG.error(e);
      return false;
    }finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  private boolean updateActivityStatistic(ActivityStatisticBean bean, String nodePath) throws Exception {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
      Node rootNode = session.getRootNode();
      Node dateNode = rootNode.getNode(nodePath);
      
      
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date() );
      dateNode.setProperty(ACTIVITY_STATISTIC_MODIFIED_DATE, cal);
      
      if(bean.getNewUser() != null && bean.getNewUser() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_NEW_USER, bean.getNewUser());
      }
      
      if(bean.getNewUserToday() != null && bean.getNewUserToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_NEW_USER_TODAY, bean.getNewUserToday());
      }
      
      if(bean.getForumActiveUserToday() != null && bean.getForumActiveUserToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_ACTIVE_USER_TODAY, bean.getForumActiveUserToday());
      }
      
      if(bean.getForumPosts() != null && bean.getForumPosts() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_POST, bean.getForumPosts());
      }
      
      if(bean.getForumPostToday() != null && bean.getForumPostToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_FORUM_POST_TODAY, bean.getForumPostToday());
      }
      
      if(bean.getLoginCount() != null && bean.getLoginCount() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_LOGIN_COUNT, bean.getLoginCount());
      }
      
      if(bean.getLoginCountToday() != null && bean.getLoginCountToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_LOGIN_COUNT_TODAY, bean.getLoginCountToday());
      }
      
      if(bean.getUserConnectionCount() != null && bean.getUserConnectionCount() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT, bean.getUserConnectionCount());
      }
      if(bean.getUserConnectionCountToday() != null && bean.getUserConnectionCountToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT_TODAY, bean.getUserConnectionCountToday());
      }
      
      if(bean.getSocialPostCount() != null && bean.getSocialPostCount() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT, bean.getSocialPostCount());
      }
      if(bean.getSocialPostCountToday() != null && bean.getSocialPostCountToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT_TODAY, bean.getSocialPostCountToday());
      }
      
      if(bean.getEmailNotificationCount() != null && bean.getEmailNotificationCount() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT, bean.getEmailNotificationCount());
      }
      if(bean.getEmailNotificationCountToday() != null && bean.getEmailNotificationCountToday() >= 0){
        dateNode.setProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT_TODAY, bean.getEmailNotificationCountToday());
      }
      
      dateNode.save();
      
      return true;
      
    }catch (Exception e){
      LOG.error(e);
      return false;
    }finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }
  
  private Node getActivityStatisticHomeNode(SessionProvider sessionProvider) throws Exception {
    ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
    return session.getRootNode().getNode(HOME);
  }
  

  @Override
  public ActivityStatisticBean getActivityStatisticByDate(Date date) throws Exception {
   
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    String dateValue = String.valueOf(calendar.get(Calendar.DATE));
    String month = String.valueOf(calendar.get(Calendar.MONTH) + 1 );
    String year = String.valueOf(calendar.get(Calendar.YEAR));

    String nodePath = HOME + "/" + year + "/" + month + "/" + dateValue;
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
      Node rootNode = session.getRootNode();
      if (rootNode.hasNode(nodePath)) {
        Node node = rootNode.getNode(nodePath);
        return fillDataToBean(node);
      }else{
        return null;
      }
    }catch (Exception e){
      LOG.error(e);
      return null;
    }finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  @Override
  public List<ActivityStatisticBean> getListActivityStatisticByDate(Date fromDate, Date toDate) throws Exception {
    List<ActivityStatisticBean> list = new ArrayList<ActivityStatisticBean>();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
        ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
        Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        String strFromDate = getStrFromDate(fromDate);
        String strToDate = getStrToDate(toDate);
        StringBuilder sb = new StringBuilder();
        String nodePath = getActivityStatisticHomeNode(sessionProvider).getPath();
        String pathPattern = buildPathPattern(nodePath);
        sb.append("SELECT * FROM " + ACTIVITY_STATISTIC_BY_DAY_NOTE_TYPE + " WHERE ");
        if(pathPattern.length() > 0) {
          sb.append(pathPattern).append(" AND ");
        }
        sb.append(" (" + ACTIVITY_STATISTIC_CREATED_DATE + " >= TIMESTAMP '" + strFromDate + "')")
          .append(" AND ")
          .append(" (" + ACTIVITY_STATISTIC_CREATED_DATE + " <= TIMESTAMP '" + strToDate + "')");
        
        QueryImpl query = (QueryImpl) queryManager.createQuery(sb.toString(), Query.SQL);
        QueryResult result = query.execute();
        NodeIterator nodeIterator = result.getNodes();
        
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            ActivityStatisticBean bean = fillDataToBean(node);
            list.add(bean);
        }
        return list;
    } catch (Exception e) {
        LOG.debug("Error while getting Activity Statistic " + e.getMessage(), e);
        return null; 
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }

    }
  }
  
  private ActivityStatisticBean fillDataToBean(Node node){
    ActivityStatisticBean bean;
    try {
      bean = new ActivityStatisticBean();
      if(node.hasProperty(ACTIVITY_STATISTIC_CREATED_DATE)){
        bean.setCreatedDate(node.getProperty(ACTIVITY_STATISTIC_CREATED_DATE).getDate().getTime());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_MODIFIED_DATE)){
        bean.setModifiedDate(node.getProperty(ACTIVITY_STATISTIC_MODIFIED_DATE).getDate().getTime());
      }
      
      //reset Bean first
      bean.setNewUser(0L);
      bean.setNewUserToday(0L);
      bean.setForumActiveUserToday(0L);
      bean.setForumPosts(0L);
      bean.setForumPostToday(0L);
      bean.setLoginCount(0L);
      bean.setLoginCountToday(0L);
      bean.setUserConnectionCount(0L);
      bean.setUserConnectionCountToday(0L);
      bean.setSocialPostCount(0L);
      bean.setSocialPostCountToday(0L);
      bean.setEmailNotificationCount(0L);
      bean.setEmailNotificationCountToday(0L);
      
      //Update Bean properties from node
      
      if(node.hasProperty(ACTIVITY_STATISTIC_NEW_USER)){
        bean.setNewUser(node.getProperty(ACTIVITY_STATISTIC_NEW_USER).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_NEW_USER_TODAY)){
        bean.setNewUserToday(node.getProperty(ACTIVITY_STATISTIC_NEW_USER_TODAY).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_FORUM_ACTIVE_USER_TODAY)){
        bean.setForumActiveUserToday(node.getProperty(ACTIVITY_STATISTIC_FORUM_ACTIVE_USER_TODAY).getLong());
      }

      if(node.hasProperty(ACTIVITY_STATISTIC_FORUM_POST)){
        bean.setForumPosts(node.getProperty(ACTIVITY_STATISTIC_FORUM_POST).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_FORUM_POST_TODAY)){
        bean.setForumPostToday(node.getProperty(ACTIVITY_STATISTIC_FORUM_POST_TODAY).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_LOGIN_COUNT)){
        bean.setLoginCount(node.getProperty(ACTIVITY_STATISTIC_LOGIN_COUNT).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_LOGIN_COUNT_TODAY)){
        bean.setLoginCountToday(node.getProperty(ACTIVITY_STATISTIC_LOGIN_COUNT_TODAY).getLong());
      }
      
      if(node.hasProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT)){
        bean.setUserConnectionCount(node.getProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT).getLong());
      }
      if(node.hasProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT_TODAY)){
        bean.setUserConnectionCountToday(node.getProperty(ACTIVITY_STATISTIC_USER_CONNECTION_COUNT_TODAY).getLong());
      }
      if(node.hasProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT)){
        bean.setSocialPostCount(node.getProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT).getLong());
      }
      if(node.hasProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT_TODAY)){
        bean.setSocialPostCountToday(node.getProperty(ACTIVITY_STATISTIC_SOCIAL_POST_COUNT_TODAY).getLong());
      }
      if(node.hasProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT)){
        bean.setEmailNotificationCount(node.getProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT).getLong());
      }
      if(node.hasProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT_TODAY)){
        bean.setEmailNotificationCountToday(node.getProperty(ACTIVITY_STATISTIC_EMAIL_NOTIFICATION_COUNT_TODAY).getLong());
      }
      
      
    } catch (Exception e) {
      LOG.error(e);
      return null;
    }
    return bean;
  }
  
  private String getStrFromDate(Date fromDate) {
    Calendar time = Calendar.getInstance();
    time.setTime(fromDate);
    Calendar theFirst = (Calendar)time.clone();
    theFirst.set(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DATE), 0, 0, 0);
    String theFirstDate = formatDateTime.format(theFirst.getTime());
    return theFirstDate + TIME_FORMAT_TAIL_FROM;
  }
  
  
  private String getStrToDate(Date toDate) {
    Calendar time = Calendar.getInstance();
    time.setTime(toDate);
    Calendar theFirst = (Calendar)time.clone();
    theFirst.set(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DATE), 23, 59, 59);
    String theFirstDate = formatDateTime.format(theFirst.getTime());
    return theFirstDate + TIME_FORMAT_TAIL_TO;
  }
  
  private String buildPathPattern(String nodePath) {
    if(nodePath.equals("/")) return "";
    return "jcr:path LIKE '" + nodePath + "/%" + "'";
  }

  @Override
  public ActivityStatisticBean startTodayStatistic() throws Exception {
    ActivityStatisticBean todayStats = new ActivityStatisticBean();
    try {
      long now = System.currentTimeMillis();
      ActivityStatisticBean yesterdayStats = this.getActivityStatisticByDate(new Date(now-DAY_IN_MILLISEC));
      
      if(null != yesterdayStats){
        todayStats.setCreatedDate(new Date(now));
        todayStats.setLoginCount(yesterdayStats.getLoginCount() + yesterdayStats.getLoginCountToday());
        todayStats.setLoginCountToday((long)0);
        todayStats.setForumPosts(yesterdayStats.getForumPosts() + yesterdayStats.getForumPostToday());
        todayStats.setForumPostToday((long)0);
        todayStats.setNewUser(yesterdayStats.getNewUser() + yesterdayStats.getNewUserToday());
        todayStats.setNewUserToday((long)0);
        todayStats.setForumActiveUserToday((long)0);
        todayStats.setUserConnectionCount(yesterdayStats.getUserConnectionCount() + yesterdayStats.getUserConnectionCountToday());
        todayStats.setUserConnectionCountToday((long)0);
        todayStats.setSocialPostCount(yesterdayStats.getSocialPostCount() + yesterdayStats.getSocialPostCountToday());
        todayStats.setSocialPostCountToday((long)0);
        todayStats.setEmailNotificationCount(yesterdayStats.getEmailNotificationCount() + yesterdayStats.getEmailNotificationCountToday());
        todayStats.setEmailNotificationCountToday((long)0);
      }else{
        todayStats.setCreatedDate(new Date(now));
        todayStats.setLoginCount((long)0);
        todayStats.setLoginCountToday((long)0);
        ForumService forumService = CommonUtils.getComponent(ForumService.class);
        if(null!=forumService){
          todayStats.setForumPosts(forumService.getForumStatistic().getPostCount());
        }else{
          todayStats.setForumPosts((long)0);
        }
        todayStats.setForumPostToday((long)0);
        todayStats.setNewUser((long)0);
        todayStats.setNewUserToday((long)0);
        todayStats.setForumActiveUserToday((long)0);
        todayStats.setUserConnectionCountToday((long)0);
        todayStats.setSocialPostCountToday((long)0);
        todayStats.setEmailNotificationCountToday((long)0);
      }
      
      this.createOrUpdate(todayStats);
      
    } catch (Exception e) {
      LOG.debug("Can not start Activity Statistic for today " + e.getMessage(), e);
      return null;
    }
    return todayStats;
  }

  @Override
  public boolean upgrade() throws Exception {
    try {
      registerNodeTypes("war:/conf/community-forums-extension/nodetypes/nodetype-activity-statistic.xml", ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      return true;
    } catch (Exception e) {
      return false;
    }

  }
  
  public void registerNodeTypes(String nodeTypeFilesName, int alreadyExistsBehaviour) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ConfigurationManager configurationService = (ConfigurationManager) container.getComponentInstanceOfType(ConfigurationManager.class);
    InputStream isXml = configurationService.getInputStream(nodeTypeFilesName);
    ExtendedNodeTypeManager ntManager = this.repositoryService.getCurrentRepository().getNodeTypeManager();
    LOG.info("\nTrying register node types from xml-file " + nodeTypeFilesName);
    ntManager.registerNodeTypes(isXml, alreadyExistsBehaviour, NodeTypeDataManager.TEXT_XML);
    LOG.info("\nNode types were registered from xml-file " + nodeTypeFilesName);
  }

}
