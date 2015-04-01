package org.exoplatform.addon.pulse.service.ws;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

@Path("ForumParticipant")
public class RestForumParticipant implements ResourceContainer{
  
  private static final String FILTER_BY_WEEK = "week";
  private static final String FILTER_BY_MONTH = "month";
  private static final String FILTER_BY_YEAR = "year";
  
  private static final SimpleDateFormat formatDateTime = new SimpleDateFormat();
  private static final String TIME_FORMAT_TAIL_TO = "T23:59:59.999";
  private static final String TIME_FORMAT_TAIL_FROM = "T00:00:00.000";
  private final long DAY_IN_MILLISEC=86400000;
  
  public static final String POST_CREATED_DATE = "exo:createdDate";
  public static final String POST_NODE_TYPE = "exo:post";
  
  private RepositoryService repositoryService;
  
  private ForumService forumService;
  
  private IdentityManager identityManager;
  
  private static final Log  LOG = ExoLogger.getLogger(RestForumParticipant.class);
  
  static {
    formatDateTime.applyPattern("yyyy-MM-dd");
  }
  
  private static final CacheControl cacheControl;
  static {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }
  private static List<String> forumParticipantsBeanTempCacheKeyQueue;
  private static Map<String, ForumParticipantsBean> forumParticipantsBeanTempCache;
  private static final int MAX_SIZE_FORUM_PARTICIPANT_BEAN_TEMP_CACHE = 100;
  private static final int MAX_RESULT = 20;
  
  public RestForumParticipant(){
    forumService = CommonsUtils.getService(ForumService.class);
    repositoryService = CommonsUtils.getService(RepositoryService.class);
    identityManager = CommonsUtils.getService(IdentityManager.class);
    if(null == forumParticipantsBeanTempCache){
      forumParticipantsBeanTempCache = new HashMap<String, RestForumParticipant.ForumParticipantsBean>();
      forumParticipantsBeanTempCacheKeyQueue = new ArrayList<String>();
    }
  }
  
  @GET
  @Path("/clean")
  @RolesAllowed({"administrators"})
  public String cleanCache(){
    forumParticipantsBeanTempCache = new HashMap<String, RestForumParticipant.ForumParticipantsBean>();
    forumParticipantsBeanTempCacheKeyQueue =  new ArrayList<String>();
    return "done";
  }

  @GET
  @Path("/ByPeriod/{filterBy}/{maxResult}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getForumParticipantByPeriod(@PathParam("filterBy") String filterBy, @PathParam("maxResult") String maxResult){
    if(null==filterBy||filterBy.length()==0){
      return Response.status(Status.BAD_REQUEST).build();
    }
    // Do not get existing data in cache;
    cleanCache();
    
    int max = 5;
    try {
      max = Integer.parseInt(maxResult);
    } catch (Exception e) {
      //do nothing
    }
    if (max > MAX_RESULT) max = MAX_RESULT;
    try{

      ForumParticipantsBean bean = getForumParticipantByPeriod(filterBy, max);
      return Response.ok(bean, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();

    }catch (Exception e){
      LOG.info(e);
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
  
  @GET
  @Path("/ByDate/{fromDateStr}/{toDateStr}/{maxResult}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getForumParticipantByDate(@PathParam("fromDateStr") String fromDateStr,
                                            @PathParam("toDateStr") String toDateStr,
                                            @PathParam("maxResult") String maxResult){
    
    // Do not get existing data in cache;
    cleanCache();
    
    Date fromDate = new Date();
    Date toDate = new Date();
    fromDate = parseDate(fromDateStr, "dd-MM-yyyy");
    toDate = parseDate(toDateStr, "dd-MM-yyyy");
    if(fromDate == null){
      return Response.status(Status.BAD_REQUEST).build();
    }
    if(toDate == null){
      toDate = new Date();
    }
    
    int max = 5;
    try {
      max = Integer.parseInt(maxResult);
    } catch (Exception e) {
      //do nothing
    }
    if (max > MAX_RESULT) max = MAX_RESULT;
    
    try {
      ForumParticipantsBean bean = getForumParticipantsBean(fromDate, toDate, max);
      return Response.ok(bean, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    } catch (Exception e) {
      LOG.info(e);
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
  
  private ForumParticipantsBean getForumParticipantByPeriod(String filterBy, int maxResult){
    Date fromDate = new Date();
    Date toDate = new Date();
    Calendar fromCal = Calendar.getInstance();
    
    //Default if filter by WEEK
    fromCal.clear();
    fromCal.setTime(toDate);
    // go to begin of current week
    fromCal.set(Calendar.DAY_OF_WEEK, fromCal.getFirstDayOfWeek());
    fromDate = fromCal.getTime();
    
    if(filterBy.equals(FILTER_BY_MONTH)){
      fromCal.clear();
      fromCal.setTime(toDate);
      // go to begin of current month
      fromCal.set(Calendar.DAY_OF_MONTH, 1);
      fromDate = fromCal.getTime();
    }
    
    if(filterBy.equals(FILTER_BY_YEAR)){
      fromCal.clear();
      fromCal.setTime(toDate);
      // go to begin of current month
      fromCal.set(Calendar.DAY_OF_YEAR, 1);
      fromDate = fromCal.getTime();
    }
    
    ForumParticipantsBean bean = getForumParticipantsBean(fromDate, toDate, maxResult);
    bean.setFilterBy(filterBy);
    return bean;
  }
  
  private ForumParticipantsBean getForumParticipantsBean(Date fromDate, Date toDate, int maxResult){
    
    LOG.info("getForumParticipant from: " + partString(fromDate, "dd-MM-yyyy")  + " to: " + partString(toDate, "dd-MM-yyyy") );
    
    StringBuffer key = new StringBuffer();
    key.append(partString(fromDate, "dd/MM/yyyy"))
       .append("-")
       .append(partString(toDate, "dd/MM/yyyy"))
       .append("-")
       .append(maxResult);
    
    ForumParticipantsBean bean =  forumParticipantsBeanTempCache.get(key.toString());
    if (null == bean){
      bean = buildForumParticipantsBean(fromDate, toDate, maxResult);
      
      if(forumParticipantsBeanTempCache.size()>=MAX_SIZE_FORUM_PARTICIPANT_BEAN_TEMP_CACHE){
        String keyOfFirstObject = forumParticipantsBeanTempCacheKeyQueue.get(0);
        LOG.info("forumParticipantsBeanTempCache is full, remove first object: " + keyOfFirstObject);
        forumParticipantsBeanTempCache.remove(keyOfFirstObject);
        forumParticipantsBeanTempCacheKeyQueue.remove(0);
      }
      
      LOG.info("put " + key.toString() + " to forumParticipantsBeanTempCache");
      
      forumParticipantsBeanTempCache.put(key.toString(), bean);
      forumParticipantsBeanTempCacheKeyQueue.add(key.toString());
      
      //LOG.info(forumParticipantsBeanTempCacheKeyQueue.toArray().toString());
    }
    return bean;
  }
  
  private ForumParticipantsBean buildForumParticipantsBean(Date fromDate, Date toDate, int maxResult){
    LOG.info("build Data from ForumData");
    ForumParticipantsBean bean = new ForumParticipantsBean();
    bean.setFromDate(fromDate);
    bean.setToDate(toDate);
    bean.setLastUpdate(new Date());
    
    List<ForumParticipantProfile> listProfile = new ArrayList<RestForumParticipant.ForumParticipantProfile>();
    
    Map<String, Long> mapForumParticipants = getPostByDate(fromDate, toDate);
    if(null != mapForumParticipants && mapForumParticipants.size()>0){
      int index = mapForumParticipants.size() -1;
      while(index >= 0 && maxResult > 0){
        try {
          String userName = mapForumParticipants.keySet().toArray()[index].toString();
          Long periodPost = mapForumParticipants.get(userName);
          ForumParticipantProfile forumParticipantProfile = buildForumParticipantProfile(userName, periodPost);
          listProfile.add(forumParticipantProfile);
          maxResult--;
          index--;
        } catch (Exception e) {
          LOG.error(e);
        }
      }
      
    }
    
    bean.setListForumParticipantProfile(listProfile);
    
    return bean;
  }
  
  private ForumParticipantProfile buildForumParticipantProfile(String userName, Long periodPost ) throws Exception{
    ForumParticipantProfile forumParticipantProfile = new ForumParticipantProfile();
    
    forumParticipantProfile.setUserName(userName);
    
    UserProfile forumProfile = forumService.getUserInfo(userName);
    forumParticipantProfile.setTotalPost(forumProfile.getTotalPost());
    forumParticipantProfile.setPeriodPost(periodPost);
    
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userName,false);
    Profile userProfile = userIdentity.getProfile();
    
    forumParticipantProfile.setAvatarUrl(userProfile.getAvatarUrl());
    forumParticipantProfile.setFullName(userProfile.getFullName());
    forumParticipantProfile.setProfileUrl(userProfile.getUrl());

    return forumParticipantProfile;
  }
  
  private Map<String, Long> getPostByDate(Date fromDate, Date toDate) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Map<String, Long> mapForumParticipant = new HashMap<String, Long>();
    try {
        ManageableRepository currentRepo = this.repositoryService.getCurrentRepository();
        Session session = sessionProvider.getSession("knowledge", currentRepo);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        String strFromDate = getStrFromDate(fromDate);
        String strToDate = getStrToDate(toDate);
        StringBuilder sb = new StringBuilder();
        String pathPattern = buildPathPattern("/exo:applications/ForumService/ForumData/CategoryHome");
        sb.append("SELECT * FROM " + POST_NODE_TYPE + " WHERE ");
        if(pathPattern.length() > 0) {
          sb.append(pathPattern).append(" AND ");
        }
        sb.append(" (" + POST_CREATED_DATE + " >= TIMESTAMP '" + strFromDate + "')")
          .append(" AND ")
          .append(" (" + POST_CREATED_DATE + " <= TIMESTAMP '" + strToDate + "')");
        
        //LOG.info(sb.toString());
        
        QueryImpl query = (QueryImpl) queryManager.createQuery(sb.toString(), Query.SQL);
        QueryResult result = query.execute();
        NodeIterator nodeIterator = result.getNodes();
        
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            String userName = node.getProperty("exo:owner").getString();
            if(mapForumParticipant.get(userName)!=null){
              Long postCount = mapForumParticipant.get(userName);
              mapForumParticipant.put(userName, postCount + 1);
            }else{
              mapForumParticipant.put(userName,1L);
            }
        }
        Map<String, Long> sortMapForumParticipant = sortByComparator(mapForumParticipant);
        
        //printMap(sortMapForumParticipant);
        
        return sortMapForumParticipant;
    } catch (Exception e) {
        LOG.debug("Error while getting Forum Participants " + e.getMessage(), e);
        return null; 
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }

    }
  }
 
  /**
   * Sort a Map by value
   * @param unsortMap
   * @return
   */
  private static Map<String, Long> sortByComparator(Map<String, Long> unsortMap) {
    
    // Convert Map to List
    List<Map.Entry<String, Long>> list = 
      new LinkedList<Map.Entry<String, Long>>(unsortMap.entrySet());
 
    // Sort list with comparator, to compare the Map values
    Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
      public int compare(Map.Entry<String, Long> o1,
                                           Map.Entry<String, Long> o2) {
        return (o1.getValue()).compareTo(o2.getValue());
      }
    });
 
    // Convert sorted map back to a Map
    Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
    for (Iterator<Map.Entry<String, Long>> it = list.iterator(); it.hasNext();) {
      Map.Entry<String, Long> entry = it.next();
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }
  
  private void printMap(Map<String, Long> map) {
    for (Map.Entry<String, Long> entry : map.entrySet()) {
      LOG.info("[Key] : " + entry.getKey() 
                                      + " [Value] : " + entry.getValue());
    }
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
  
  private Date parseDate(String date,String formatStr){
    try {      
      SimpleDateFormat format = new SimpleDateFormat(formatStr);      
      Date ret = format.parse(date);        
      return ret;
    } catch (Exception e) {
      return null;
    } 
  }
  
  private String partString(Date date, String format){
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    String s = formatter.format(date);
    return s;
  }
  
  
  public class ForumParticipantsBean{
    
    private Date fromDate;
    private Date toDate;
    private String filterBy;
    private Date lastUpdate;
    
    List<ForumParticipantProfile> listForumParticipantProfile;
    
    public Date getFromDate() {
      return fromDate;
    }
    public void setFromDate(Date fromDate) {
      this.fromDate = fromDate;
    }
    public Date getToDate() {
      return toDate;
    }
    public void setToDate(Date toDate) {
      this.toDate = toDate;
    }
    public String getFilterBy() {
      return filterBy;
    }
    public void setFilterBy(String filterBy) {
      this.filterBy = filterBy;
    }
    
    public Date getLastUpdate() {
      return lastUpdate;
    }
    public void setLastUpdate(Date lastUpdate) {
      this.lastUpdate = lastUpdate;
    }
    public List<ForumParticipantProfile> getListForumParticipantProfile() {
      return listForumParticipantProfile;
    }
    public void setListForumParticipantProfile(List<ForumParticipantProfile> listForumParticipantProfile) {
      this.listForumParticipantProfile = listForumParticipantProfile;
    }
    
    
  }
  public class ForumParticipantProfile{
    
    private String userName;
    private String fullName;
    private String avatarUrl;
    private String profileUrl;
    private long totalPost;
    private long periodPost;
    
    public String getUserName() {
      return userName;
    }
    public void setUserName(String userName) {
      this.userName = userName;
    }
    public String getFullName() {
      return fullName;
    }
    public void setFullName(String fullName) {
      this.fullName = fullName;
    }
    public String getAvatarUrl() {
      return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl;
    }
    public long getTotalPost() {
      return totalPost;
    }
    public void setTotalPost(long totalPost) {
      this.totalPost = totalPost;
    }
    public long getPeriodPost() {
      return periodPost;
    }
    public void setPeriodPost(long periodPost) {
      this.periodPost = periodPost;
    }
    public String getProfileUrl() {
      return profileUrl;
    }
    public void setProfileUrl(String profileUrl) {
      this.profileUrl = profileUrl;
    }
  }

}
