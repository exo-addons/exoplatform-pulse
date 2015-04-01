/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.addon.pulse.service.ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticBean;
import org.exoplatform.addon.pulse.service.activitystatistic.ActivityStatisticService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;

import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

@Path("activitystatistic")
@Produces(MediaType.APPLICATION_JSON)
public class RestActivitiesStatistic implements ResourceContainer {
  private static final String FILTER_BY_DAY = "day";
  private static final String FILTER_BY_WEEK = "week";
  private static final String FILTER_BY_MONTH = "month";
  private static HashMap<String, CommunityStatistic> communityStatisticData;
  
  private static final Log LOG = ExoLogger.getLogger(RestActivitiesStatistic.class);
  private ActivityStatisticService service;

  private static final CacheControl cacheControl;
  static {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }
  public RestActivitiesStatistic(){
    service = (ActivityStatisticService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityStatisticService.class);
    communityStatisticData = new HashMap<String, RestActivitiesStatistic.CommunityStatistic>();
  }
  
  @GET
  @Path("/upgrade")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({"administrators"})
  public Response migrateData(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo) throws Exception {
    
     try{
       boolean doUpgrade =service.upgrade();
       if(doUpgrade) return Response.ok("Migrate successfully", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
       return Response.ok("Migrate failured", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
       
     }catch (Exception e){
       LOG.info(e);
       return Response.ok("Migrate failured", MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
     }
  }
  
  @GET
  @Path("/export/csv/{maxColumn}/{filter}/{from}")
  @Produces("application/csv")
  public Response getExportCsv(@Context SecurityContext sc, 
                               @Context UriInfo uriInfo,
                               @PathParam("maxColumn") String maxColumn,
                               @PathParam("filter") String filter,
                               @PathParam("from") String from,
                               @QueryParam("exportNewUsersData") String exportNewUsersData,
                               @QueryParam("exportLoginCountData") String exportLoginCountData,
                               @QueryParam("exportForumActiveUsersData") String exportForumActiveUsersData,
                               @QueryParam("exportNewForumPostsData") String exportNewForumPostsData,
                               @QueryParam("exportPlfDownloadsData") String exportPlfDownloadsData,
                               @QueryParam("exportUserConnectionData") String exportUserConnectionData,
                               @QueryParam("exportSocialPostData") String exportSocialPostData,
                               @QueryParam("exportEmailNotificationData") String exportEmailNotificationData) throws Exception {

    if(null==maxColumn||maxColumn.length()==0 ||
        null==filter||filter.length()==0||
        null==from||from.length()==0){
      return Response.status(Status.BAD_REQUEST).build();
    }

    boolean isExportNewUsersData = false;
    boolean isExportLoginCountData = false;
    boolean isExportForumActiveUsersData = false;
    boolean isExportNewForumPostsData = false;
    boolean isExportPlfDownloadsData = false;
    boolean isExportUserConnectionData = false;
    boolean isExportSocialPostData = false;
    boolean isExportEmailNotificationData = false;

    if(null!=exportNewUsersData && exportNewUsersData.equalsIgnoreCase("true")) isExportNewUsersData=true;
    if(null!=exportLoginCountData && exportLoginCountData.equalsIgnoreCase("true")) isExportLoginCountData=true;
    if(null!=exportForumActiveUsersData && exportForumActiveUsersData.equalsIgnoreCase("true")) isExportForumActiveUsersData=true;
    if(null!=exportNewForumPostsData && exportNewForumPostsData.equalsIgnoreCase("true")) isExportNewForumPostsData=true;
    if(null!=exportPlfDownloadsData && exportPlfDownloadsData.equalsIgnoreCase("true")) isExportPlfDownloadsData=true;
    
    if(null!=exportUserConnectionData && exportUserConnectionData.equalsIgnoreCase("true")) isExportUserConnectionData=true;
    if(null!=exportSocialPostData && exportSocialPostData.equalsIgnoreCase("true")) isExportSocialPostData=true;
    if(null!=exportEmailNotificationData && exportEmailNotificationData.equalsIgnoreCase("true")) isExportEmailNotificationData=true;

    Date fromDate = parseDate(from);
    String fileName = "export_" + maxColumn + "_" +filter + "_from_" + partString(fromDate, "yyyy.MM.dd") +".csv";
    try{
      HSSFWorkbook workbook = buildExportDataForExcel(maxColumn, filter, fromDate,
                                                      isExportNewUsersData,
                                                      isExportLoginCountData,
                                                      isExportForumActiveUsersData,
                                                      isExportNewForumPostsData,
                                                      isExportPlfDownloadsData,
                                                      isExportUserConnectionData,
                                                      isExportSocialPostData,
                                                      isExportEmailNotificationData);
      byte[] csvContent = buildCsvContent(workbook).getBytes();
      ResponseBuilder response = Response.ok((Object) csvContent);
      response.header("Content-Disposition", "attachment; filename=" + fileName);
      return response.build();
      
    }catch (Exception e){
      LOG.info(e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
  
  @GET
  @Path("/export/excel/{maxColumn}/{filter}/{from}")
  @Produces("application/vnd.ms-excel")
  public Response getExportExcel(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo,
                                  @PathParam("maxColumn") String maxColumn,
                                  @PathParam("filter") String filter,
                                  @PathParam("from") String from,
                                  @QueryParam("exportNewUsersData") String exportNewUsersData,
                                  @QueryParam("exportLoginCountData") String exportLoginCountData,
                                  @QueryParam("exportForumActiveUsersData") String exportForumActiveUsersData,
                                  @QueryParam("exportNewForumPostsData") String exportNewForumPostsData,
                                  @QueryParam("exportPlfDownloadsData") String exportPlfDownloadsData,
                                  @QueryParam("exportUserConnectionData") String exportUserConnectionData,
                                  @QueryParam("exportSocialPostData") String exportSocialPostData,
                                  @QueryParam("exportEmailNotificationData") String exportEmailNotificationData) throws Exception {
    
     if(null==maxColumn||maxColumn.length()==0 ||
        null==filter||filter.length()==0||
        null==from||from.length()==0){
       return Response.status(Status.BAD_REQUEST).build();
     }
     
     boolean isExportNewUsersData = false;
     boolean isExportLoginCountData = false;
     boolean isExportForumActiveUsersData = false;
     boolean isExportNewForumPostsData = false;
     boolean isExportPlfDownloadsData = false;
     boolean isExportUserConnectionData = false;
     boolean isExportSocialPostData = false;
     boolean isExportEmailNotificationData = false;
     
     if(null!=exportNewUsersData && exportNewUsersData.equalsIgnoreCase("true")) isExportNewUsersData=true;
     if(null!=exportLoginCountData && exportLoginCountData.equalsIgnoreCase("true")) isExportLoginCountData=true;
     if(null!=exportForumActiveUsersData && exportForumActiveUsersData.equalsIgnoreCase("true")) isExportForumActiveUsersData=true;
     if(null!=exportNewForumPostsData && exportNewForumPostsData.equalsIgnoreCase("true")) isExportNewForumPostsData=true;
     if(null!=exportPlfDownloadsData && exportPlfDownloadsData.equalsIgnoreCase("true")) isExportPlfDownloadsData=true;
     
     if(null!=exportUserConnectionData && exportUserConnectionData.equalsIgnoreCase("true")) isExportUserConnectionData=true;
     if(null!=exportSocialPostData && exportSocialPostData.equalsIgnoreCase("true")) isExportSocialPostData=true;
     if(null!=exportEmailNotificationData && exportEmailNotificationData.equalsIgnoreCase("true")) isExportEmailNotificationData=true;
    
     try{
        Date fromDate = parseDate(from);
        String fileName = "export_" + maxColumn + "_" +filter + "_from_" + partString(fromDate, "yyyy.MM.dd") +".xls";
               
        HSSFWorkbook hssfWorkbook = buildExportDataForExcel(maxColumn, filter, fromDate,
                                                                   isExportNewUsersData,
                                                                   isExportLoginCountData,
                                                                   isExportForumActiveUsersData,
                                                                   isExportNewForumPostsData,
                                                                   isExportPlfDownloadsData,
                                                                   isExportUserConnectionData,
                                                                   isExportSocialPostData,
                                                                   isExportEmailNotificationData);
            
        File tempFile = File.createTempFile(fileName, ".tmp"); 
        
        FileOutputStream fileOut = new FileOutputStream(tempFile);
        
        hssfWorkbook.write(fileOut);
        fileOut.flush();
        fileOut.close();
        tempFile.deleteOnExit();
                  
        
        ResponseBuilder response = Response.ok((Object) tempFile);
        response.header("Content-Disposition", "attachment; filename=" + fileName);
        return response.build();
        
        //Do not response download stream because downloaded file can not be opened by MS-Office
        /*ResponseBuilder response = Response.ok((Object)  hssfWorkbook.getBytes());
        response.header("Content-Disposition", "attachment; filename=" + fileName);
        return response.build();*/
       
     }catch (Exception e){
       LOG.info(e.getStackTrace());
       return Response.status(Status.BAD_REQUEST).build();
     }
     
  }
  
  @GET
  @Path("/plfdownload/{maxColumn}/{filter}/{from}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPLFdowloadStatisticByFilter(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo,
                                  @PathParam("maxColumn") String maxColumn,
                                  @PathParam("filter") String filter,
                                  @PathParam("from") String from) throws Exception {
    
    if(null==maxColumn||maxColumn.length()==0 ||
        null==filter||filter.length()==0||
        null==from||from.length()==0){
       return Response.status(Status.BAD_REQUEST).build();
     }
     try{
     
       Date fromDate = parseDate(from);
       ChartData chartData = buildPlfDownloadData(maxColumn, filter, fromDate);
       return Response.ok(chartData, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
       
     }catch (Exception e){
       LOG.info(e);
       return Response.status(Status.BAD_REQUEST).build();
     }
  }
  
  @GET
  @Path("/{maxColumn}/{filter}/{from}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatisticByFilter(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo,
                                  @PathParam("maxColumn") String maxColumn,
                                  @PathParam("filter") String filter,
                                  @PathParam("from") String from) throws Exception {
    
    if(null==maxColumn||maxColumn.length()==0 ||
       null==filter||filter.length()==0||
       null==from||from.length()==0){
      return Response.status(Status.BAD_REQUEST).build();
    }
    try{
    
      Date fromDate = parseDate(from);
      ChartData chartData = buildStatisticByFilter(maxColumn, filter, fromDate);
      return Response.ok(chartData, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
      
    }catch (Exception e){
      LOG.info(e);
      return Response.status(Status.BAD_REQUEST).build();
    }
 }
 
  
  @GET
  @Path("/all/{from}/{to}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllStatistic(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo,  
                                  @PathParam("from") String from, 
                                  @PathParam("to") String to) throws Exception {
    Date fromDate = parseDate(from);
    Date toDate = parseDate(to);
    List<ActivityStatisticBean> list = service.getListActivityStatisticByDate(fromDate, toDate);
    return Response.ok(list, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
 }
  
  @GET
  @Path("/data-statistic-currentweek")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCommunityStatisticCurrentWeek(@Context SecurityContext sc, 
                                  @Context UriInfo uriInfo) throws Exception {

    String today = partString(new Date(), "yyyy-MM-dd");
    CommunityStatistic communityStatistic = communityStatisticData.get(today);
    
    if(null != communityStatistic){
      return Response.ok(communityStatistic, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
    }
    
    communityStatistic = new CommunityStatistic();
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, -6);  //back to last 7 days
    
    ChartData last7DaysData = buildStatisticByFilter("7", FILTER_BY_DAY, cal.getTime());
    ChartData weekPlfDownloadData = buildPlfDownloadData("2", FILTER_BY_WEEK, cal.getTime());
    
    communityStatistic.setTotalDownload(weekPlfDownloadData.getPlfDownloadsData().get(0));
    Long last7DaysTotalPost =0L;
    List<Long> listPost = last7DaysData.getNewForumPostsData();
    for (Long obj : listPost) {
      last7DaysTotalPost += obj;
    }

    communityStatistic.setTotalPost(last7DaysTotalPost);
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityManager identityManager = (IdentityManager) container
        .getComponentInstanceOfType(IdentityManager.class);
    ListAccess<Identity> listAccess = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, new ProfileFilter(),
                false);
    communityStatistic.setTotalMember(listAccess.getSize());
    
    communityStatisticData.put(today, communityStatistic);
    
    return Response.ok(communityStatistic, MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
 }
  
  private Date parseDate(String date){
    try {      
      SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");      
      Date ret = format.parse(date);        
      return ret;
    } catch (Exception e) {
      return null;
    } 
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
  
  private String buildCsvContent(HSSFWorkbook workbook){
    HSSFSheet sheet = workbook.getSheetAt(0);
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
        HSSFRow row = sheet.getRow(i);
        for (int j = 0; j < row.getLastCellNum(); j++) {
            HSSFCell cell = row.getCell(j);
            int cellType = cell.getCellType();
            if(cellType == HSSFCell.CELL_TYPE_STRING){
              buffer.append(cell.getStringCellValue());
            }else if(cellType == HSSFCell.CELL_TYPE_NUMERIC){
              buffer.append( new DecimalFormat("#").format(cell.getNumericCellValue()));
            }
            if(j<row.getLastCellNum()-1){
              buffer.append(',');
            }
        }
        buffer.append('\n');
    }
    return buffer.toString();
  }
  
  private HSSFWorkbook buildExportDataForExcel(String maxColumn, String filter,Date fromDate,
                                               boolean isExportNewUsersData,
                                               boolean isExportLoginCountData,
                                               boolean isExportForumActiveUsersData,
                                               boolean isExportNewForumPostsData,
                                               boolean isExportPlfDownloadsData,
                                               boolean isExportUserConnectionData,
                                               boolean isExportSocialPostData,
                                               boolean isExportEmailNotificationData) throws Exception{
    ChartData data = buildExportData(maxColumn, filter, fromDate, isExportPlfDownloadsData);
    String fileName = "export_" + maxColumn + "_" +filter + "_from_" + partString(fromDate, "yyyy.MM.dd") +".xls";
    
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet worksheet = workbook.createSheet(fileName);

    // index from 0,0... cell A1 is cell(0,0)
    
    HSSFCellStyle headerCellStyle = workbook.createCellStyle();
    HSSFFont hSSFFont = workbook.createFont();
    hSSFFont.setFontName(HSSFFont.FONT_ARIAL);
    hSSFFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
    hSSFFont.setColor(HSSFColor.BLACK.index);
    headerCellStyle.setFont(hSSFFont);
    
    HSSFRow headerRow = worksheet.createRow((short) 0);
    HSSFCell cellA1=headerRow.createCell(0);
    cellA1.setCellValue("Statistics");
    cellA1.setCellStyle(headerCellStyle);
    
    for (int i=0; i < data.getListTitle().size(); i++) {
      HSSFCell cell = headerRow.createCell(i+1);
      cell.setCellValue(data.getListTitle().get(i));
      cell.setCellStyle(headerCellStyle);
    }
    
    HSSFCell totalCell=headerRow.createCell(data.getListTitle().size()+1);
    totalCell.setCellValue("Total");
    totalCell.setCellStyle(headerCellStyle);
    
    int rowIndex=1;
    if(isExportNewUsersData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell = metricRow.createCell(0);
      metricCell.setCellValue("New Users Registration");
      Long total=0L;
      for (int i=0; i < data.getNewUsersData().size(); i++) {
        total += data.getNewUsersData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getNewUsersData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getNewUsersData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    if(isExportLoginCountData){
      HSSFRow metricRow = worksheet.createRow( rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("Nb Unique Login");
      Long total=0L;
      for (int i=0; i < data.getLoginCountData().size(); i++) {
        total += data.getLoginCountData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getLoginCountData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getLoginCountData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    if(isExportForumActiveUsersData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("Forum Active Users Average");
      Long total=0L;
      int itemHasData = 0;
      for (int i=0; i < data.getForumActiveUsersData().size(); i++) {
        total += data.getForumActiveUsersData().get(i);
        if(data.getForumActiveUsersData().get(i)>0) itemHasData++;
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getForumActiveUsersData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getForumActiveUsersData().size() + 1);
      Long average = itemHasData>0?total/itemHasData:0L;
      metricTotalCell.setCellValue(average);
      
      rowIndex++;
    }
    
    if(isExportNewForumPostsData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("New Forum Posts");
      Long total=0L;
      for (int i=0; i < data.getNewForumPostsData().size(); i++) {
        total += data.getNewForumPostsData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getNewForumPostsData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getNewForumPostsData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    if(isExportUserConnectionData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("New User connections");
      Long total=0L;
      for (int i=0; i < data.getUserConnectionData().size(); i++) {
        total += data.getUserConnectionData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getUserConnectionData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getUserConnectionData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    if(isExportSocialPostData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("New posts in activities stream");
      Long total=0L;
      for (int i=0; i < data.getSocialPostData().size(); i++) {
        total += data.getSocialPostData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getSocialPostData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getSocialPostData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    if(isExportEmailNotificationData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("Number of notification emails sent");
      Long total=0L;
      for (int i=0; i < data.getEmailNotificationData().size(); i++) {
        total += data.getEmailNotificationData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getEmailNotificationData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getEmailNotificationData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    
    
    if(isExportPlfDownloadsData){
      HSSFRow metricRow = worksheet.createRow(rowIndex);
      HSSFCell metricCell=metricRow.createCell(0);
      metricCell.setCellValue("PLF Downloads");
      Long total=0L;
      for (int i=0; i < data.getPlfDownloadsData().size(); i++) {
        total += data.getPlfDownloadsData().get(i);
        HSSFCell cell = metricRow.createCell(i+1);
        cell.setCellValue(data.getPlfDownloadsData().get(i));
      }
      HSSFCell metricTotalCell = metricRow.createCell(data.getPlfDownloadsData().size() + 1);
      metricTotalCell.setCellValue(total);
      
      rowIndex++;
    }
    return workbook;
  }
  
  private ChartData buildPlfDownloadData(String maxColumn, String filter, Date fromDate){
    int totalDataCoulumn = 5;
    try{
      totalDataCoulumn = Integer.parseInt(maxColumn);
    }catch (Exception e){
      //do nothing
    }
    if(filter.equalsIgnoreCase(FILTER_BY_DAY)){
      List<String> listTitle = new ArrayList<String>();
      List<Long> plfDownloadsData = new ArrayList<Long>();
      
      for(int i=0; i<totalDataCoulumn; i++){
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(fromDate);
        calendar.add(Calendar.DATE, i);
        Date toDate = calendar.getTime();
        String toDateStr =  partString(toDate, "yyyy-MM-dd");
        
        listTitle.add(partString(toDate, "dd-MM-yyyy"));
        String url="http://sourceforge.net/projects/exo/files/stats/json?start_date="+toDateStr+"&end_date="+ toDateStr;
        try{
         String returnValue = httpGet(url);
         plfDownloadsData.add(getTotalPlfDownloadDataFromJson(returnValue,toDate));
        }catch(Exception e){
          LOG.error(e.getMessage());
          plfDownloadsData.add(0l);
        }
        
      }
      ChartData chartData = new ChartData();
      chartData.setListTitle(listTitle);
      chartData.setPlfDownloadsData(plfDownloadsData);
      return chartData;
    }
    
    
    if(filter.equalsIgnoreCase(FILTER_BY_WEEK)){
      List<String> listTitle = new ArrayList<String>();
      List<Long> plfDownloadsData = new ArrayList<Long>();
      
      String fromDateStr = partString(fromDate,"yyyy-MM-dd");
      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTime(fromDate);
      int fromWeek = calendar.get(Calendar.WEEK_OF_YEAR);
      int fromMonth = calendar.get(Calendar.MONTH);
      if(fromMonth==Calendar.DECEMBER && fromWeek==1) fromWeek =53; 
      int fromYear = calendar.get(Calendar.YEAR);
      calendar.clear();
      calendar.set(Calendar.WEEK_OF_YEAR,fromWeek);
      calendar.set(Calendar.YEAR,fromYear);
      Date beginWeekDate = calendar.getTime();
      calendar.clear();
      calendar.setTime(beginWeekDate);
      calendar.add(Calendar.DATE, 6);
      Date endWeekDate = calendar.getTime();
      calendar.clear();
      String toDateStr = partString(endWeekDate,"yyyy-MM-dd");
      for(int i=0; i< totalDataCoulumn; i++){
        calendar.setTime(endWeekDate);
        listTitle.add("W" + calendar.get(Calendar.WEEK_OF_YEAR) + "-" + calendar.get(Calendar.YEAR));
        String url="http://sourceforge.net/projects/exo/files/stats/json?start_date="+fromDateStr+"&end_date="+ toDateStr;
        try{
         String returnValue = httpGet(url);
         plfDownloadsData.add(getTotalPlfDownloadDataFromJson(returnValue, parseDate(fromDateStr, "yyyy-MM-dd")));
        }catch(Exception e){
          LOG.error(e.getMessage());
          plfDownloadsData.add(0l);
        }
       
        calendar.clear();
        
        fromDate = endWeekDate;
        calendar.setTime(fromDate);
        calendar.add(Calendar.DATE, 1);
        fromDate = calendar.getTime();
        fromDateStr = partString(fromDate,"yyyy-MM-dd");
        
        calendar.add(Calendar.DATE, 6);
        endWeekDate = calendar.getTime();
        toDateStr = partString(endWeekDate,"yyyy-MM-dd");
        calendar.clear();
      }
      ChartData chartData = new ChartData();
      chartData.setListTitle(listTitle);
      chartData.setPlfDownloadsData(plfDownloadsData);
      return chartData;
    }
    
    
    if(filter.equalsIgnoreCase(FILTER_BY_MONTH)){
      List<String> listTitle = new ArrayList<String>();
      List<Long> plfDownloadsData = new ArrayList<Long>();
      
      String fromDateStr = partString(fromDate,"yyyy-MM-dd");
      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTime(fromDate);

      int fromMonth = calendar.get(Calendar.MONTH);
      int fromYear = calendar.get(Calendar.YEAR);
      //calendar.add(Calendar.MONTH, totalDataCoulumn);
      //int toYear = calendar.get(Calendar.YEAR);
      calendar.clear();
      calendar.set(Calendar.MONTH,fromMonth);
      calendar.set(Calendar.YEAR,fromYear);
      Date beginMonthDate = calendar.getTime();
      calendar.clear();
      calendar.setTime(beginMonthDate);
      int dayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
      calendar.add(Calendar.DATE,dayOfMonth - 1);
      Date endMonthDate = calendar.getTime();
      calendar.clear();
      String toDateStr = partString(endMonthDate,"yyyy-MM-dd");
      for(int i=0; i< totalDataCoulumn; i++){
        calendar.setTime(endMonthDate);
        listTitle.add(partString(calendar.getTime(), "MMM") + "-" + calendar.get(Calendar.YEAR));
        String url="http://sourceforge.net/projects/exo/files/stats/json?start_date="+fromDateStr+"&end_date="+ toDateStr;
        try{
         String returnValue = httpGet(url);
         plfDownloadsData.add(getTotalPlfDownloadDataFromJson(returnValue, parseDate(fromDateStr, "yyyy-MM-dd")));
        }catch(Exception e){
          plfDownloadsData.add(0l);
        }
       
        calendar.clear();
        
        fromDate = endMonthDate;
        calendar.setTime(fromDate);
        calendar.add(Calendar.DATE, 1);
        fromDate = calendar.getTime();
        fromDateStr = partString(fromDate,"yyyy-MM-dd");
        
        dayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DATE, dayOfMonth-1);
        endMonthDate = calendar.getTime();
        toDateStr = partString(endMonthDate,"yyyy-MM-dd");
        calendar.clear();
      }
      ChartData chartData = new ChartData();
      chartData.setListTitle(listTitle);
      chartData.setPlfDownloadsData(plfDownloadsData);
      return chartData;
    }
    
    return null;
  }
  
  private ChartData buildExportData(String maxColumn, String filter,Date fromDate, boolean isExportPlfDownloadsData) throws Exception{
    ChartData platformStatisticData = buildStatisticByFilter(maxColumn, filter, fromDate);
    if(isExportPlfDownloadsData){
      ChartData platformDownloadStatisticData = buildPlfDownloadData(maxColumn, filter, fromDate);
      
      platformStatisticData.setPlfDownloadsData(platformDownloadStatisticData.getPlfDownloadsData());
    }
    return platformStatisticData;
  }
  
  private ChartData buildStatisticByFilter(String maxColumn, String filter,Date fromDate) throws Exception{
    
    int totalDataCoulumn = 5;
    try{
      totalDataCoulumn = Integer.parseInt(maxColumn);
    }catch (Exception e){
      //do nothing
    }
    
    if(filter.equalsIgnoreCase(FILTER_BY_DAY)){
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(fromDate);
      calendar.add(Calendar.DATE, totalDataCoulumn-1);
      Date toDate = calendar.getTime();
      
      List<ActivityStatisticBean> list = service.getListActivityStatisticByDate(fromDate, toDate);
      TreeMap<Date,ActivityStatisticBean> dateData = new TreeMap<Date, ActivityStatisticBean>();
      //init empty-data
      for(int i=0; i<totalDataCoulumn; i++){
        calendar.clear();
        calendar.setTime(fromDate);
        calendar.add(Calendar.DATE, i);
        Date nextDate = parseDate(partString(calendar.getTime(), "dd/MM/yyyy"), "dd/MM/yyyy");
        dateData.put(nextDate, null);
      }
      
      List<String> listTitle = new ArrayList<String>();
      List<Long> newUsersData = new ArrayList<Long>();
      List<Long> loginCountData = new ArrayList<Long>();
      List<Long> forumActiveUsersData = new ArrayList<Long>();
      List<Long> newForumPostsData = new ArrayList<Long>();
      
      List<Long> userConnectionData = new ArrayList<Long>();
      List<Long> socialPostData = new ArrayList<Long>();
      List<Long> emailNotificationData = new ArrayList<Long>();
      
      ChartData chartData = new ChartData();
      
      for (ActivityStatisticBean bean : list) {
        dateData.put(parseDate(partString(bean.getCreatedDate(), "dd/MM/yyyy"), "dd/MM/yyyy"), bean);
      }
      
      for(Date key: dateData.keySet()){
        ActivityStatisticBean bean = dateData.get(key);
        if(bean!=null){
          listTitle.add(partString(bean.getCreatedDate(),"dd-MM-yyyy"));
          newUsersData.add(bean.getNewUserToday());
          loginCountData.add(bean.getLoginCountToday());
          forumActiveUsersData.add(bean.getForumActiveUserToday());
          newForumPostsData.add(bean.getForumPostToday());
          
          userConnectionData.add(bean.getUserConnectionCountToday());
          socialPostData.add(bean.getSocialPostCountToday());
          emailNotificationData.add(bean.getEmailNotificationCountToday());
        }else{
          listTitle.add(partString(key,"dd-MM-yyyy"));
          newUsersData.add(0L);
          loginCountData.add(0L);
          forumActiveUsersData.add(0L);
          newForumPostsData.add(0L);
          userConnectionData.add(0L);
          socialPostData.add(0L);
          emailNotificationData.add(0L);
        }
      }
      
      chartData.setListTitle(listTitle);
      chartData.setNewUsersData(newUsersData);
      chartData.setLoginCountData(loginCountData);
      chartData.setForumActiveUsersData(forumActiveUsersData);
      chartData.setNewForumPostsData(newForumPostsData);
      
      chartData.setUserConnectionData(userConnectionData);
      chartData.setSocialPostData(socialPostData);
      chartData.setEmailNotificationData(emailNotificationData);
      return chartData;
    }
    
    if(filter.equalsIgnoreCase(FILTER_BY_WEEK)){
      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTime(fromDate);
      calendar.add(Calendar.WEEK_OF_YEAR, totalDataCoulumn -1);
      Date nextFewWeek = calendar.getTime();

      List<ActivityStatisticBean> list = service.getListActivityStatisticByDate(fromDate, nextFewWeek);
      
      List<String> listTitle = new ArrayList<String>();
      List<Long> newUsersData = new ArrayList<Long>();
      List<Long> loginCountData = new ArrayList<Long>();
      List<Long> forumActiveUsersData = new ArrayList<Long>();
      List<Long> newForumPostsData = new ArrayList<Long>();
      
      List<Long> userConnectionData = new ArrayList<Long>();
      List<Long> socialPostData = new ArrayList<Long>();
      List<Long> emailNotificationData = new ArrayList<Long>();
      
      ChartData chartData = new ChartData();
      
      TreeMap<String,List<ActivityStatisticBean>> weekData = new TreeMap<String, List<ActivityStatisticBean>>();
      //init empty-data
      for(int i=0; i< totalDataCoulumn; i++){
        calendar.clear();
        calendar.setTime(fromDate);
        calendar.add(Calendar.WEEK_OF_YEAR, i);
        int weekIndex = calendar.get(Calendar.WEEK_OF_YEAR);
        int monthIndex = calendar.get(Calendar.MONTH);
        if(monthIndex == Calendar.DECEMBER && weekIndex==1) weekIndex =53; 
        int year = calendar.get(Calendar.YEAR);
        //goto begin of week
        calendar.clear();
        calendar.set(Calendar.WEEK_OF_YEAR, weekIndex);
        calendar.set(Calendar.YEAR, year);
        //goto end of week
        calendar.add(Calendar.DATE, 6);
        
        String week = "";
        if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER && calendar.get(Calendar.WEEK_OF_YEAR)==1){
          week = 53 + "-" + calendar.get(Calendar.YEAR);
        }else{
          week = calendar.get(Calendar.WEEK_OF_YEAR) + "-" + calendar.get(Calendar.YEAR);
        }
        week = week.length()<7? calendar.get(Calendar.YEAR) + "-" +"0" + week: calendar.get(Calendar.YEAR) + "-" +week;
        weekData.put(week, new ArrayList<ActivityStatisticBean>());
      }
      
      for (ActivityStatisticBean bean : list) {
        calendar.clear();    
        calendar.setTime(bean.getCreatedDate());
        
        int weekIndex = calendar.get(Calendar.WEEK_OF_YEAR);
        int monthIndex = calendar.get(Calendar.MONTH);
        if(monthIndex == Calendar.DECEMBER && weekIndex==1) weekIndex =53; 
        int year = calendar.get(Calendar.YEAR);
        //goto begin of week
        calendar.clear();
        calendar.set(Calendar.WEEK_OF_YEAR, weekIndex);
        calendar.set(Calendar.YEAR, year);
        //goto end of week
        calendar.add(Calendar.DATE, 6);
        
        String week = "";
        if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER && calendar.get(Calendar.WEEK_OF_YEAR)==1){
          week = 53 + "-" + calendar.get(Calendar.YEAR);
        }else{
          week = calendar.get(Calendar.WEEK_OF_YEAR) + "-" + calendar.get(Calendar.YEAR);
        }
        week = week.length()<7? calendar.get(Calendar.YEAR) + "-" +"0" + week: calendar.get(Calendar.YEAR) + "-" +week;
         
        if(weekData.containsKey(week)){
          List<ActivityStatisticBean> listValueOfNode = weekData.get(week);
          listValueOfNode.add(bean);
        }else{
          List<ActivityStatisticBean> listValueOfNode = new ArrayList<ActivityStatisticBean>();
          listValueOfNode.add(bean);
          weekData.put(week, listValueOfNode);
        }
      }
      
      for(String key: weekData.keySet()){
        List<ActivityStatisticBean> listValueOfNode = weekData.get(key);
        Long weekNewUsersValue = 0L;
        Long weekLoginCountValue =0L;
        Long weekForumActiveUsersValue = 0L;
        Long weekNewForumPostsValue = 0L;
        
        Long weekUserConnectionValue = 0L;
        Long weekSocialPostsValue = 0L;
        Long weekEmailNotificationValue = 0L;
        
        for (ActivityStatisticBean obj : listValueOfNode) {
          weekNewUsersValue = weekNewUsersValue + obj.getNewUserToday();
          weekLoginCountValue = weekLoginCountValue + obj.getLoginCountToday();
          weekForumActiveUsersValue = weekForumActiveUsersValue + obj.getForumActiveUserToday();
          weekNewForumPostsValue = weekNewForumPostsValue + obj.getForumPostToday();
          
          weekUserConnectionValue = weekUserConnectionValue + obj.getUserConnectionCountToday();
          weekSocialPostsValue= weekSocialPostsValue + obj.getSocialPostCountToday();
          weekEmailNotificationValue = weekEmailNotificationValue + obj.getEmailNotificationCountToday();
        }
        
        String weekTitle = "W" + key.substring(5,key.length());
        listTitle.add(weekTitle);
        newUsersData.add(weekNewUsersValue);
        loginCountData.add(weekLoginCountValue);
        forumActiveUsersData.add(weekForumActiveUsersValue>0?(Long)(weekForumActiveUsersValue/listValueOfNode.size()):0L);
        newForumPostsData.add(weekNewForumPostsValue);
        
        userConnectionData.add(weekUserConnectionValue);
        socialPostData.add(weekSocialPostsValue);
        emailNotificationData.add(weekEmailNotificationValue);
      }
      chartData.setListTitle(listTitle);
      chartData.setNewUsersData(newUsersData);
      chartData.setLoginCountData(loginCountData);
      chartData.setForumActiveUsersData(forumActiveUsersData);
      chartData.setNewForumPostsData(newForumPostsData);
      
      chartData.setUserConnectionData(userConnectionData);
      chartData.setSocialPostData(socialPostData);
      chartData.setEmailNotificationData(emailNotificationData);
      
      return chartData;
    }
    
    if(filter.equalsIgnoreCase(FILTER_BY_MONTH)){
      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTime(fromDate);
      calendar.add(Calendar.MONTH, totalDataCoulumn-1);
      Date nextFewMonth = calendar.getTime();

      List<ActivityStatisticBean> list = service.getListActivityStatisticByDate(fromDate, nextFewMonth);
      
      List<String> listTitle = new ArrayList<String>();
      List<Long> newUsersData = new ArrayList<Long>();
      List<Long> loginCountData = new ArrayList<Long>();
      List<Long> forumActiveUsersData = new ArrayList<Long>();
      List<Long> newForumPostsData = new ArrayList<Long>();
      
      List<Long> userConnectionData = new ArrayList<Long>();
      List<Long> socialPostData = new ArrayList<Long>();
      List<Long> emailNotificationData = new ArrayList<Long>();
      
      ChartData chartData = new ChartData();
      
      TreeMap<String,List<ActivityStatisticBean>> monthData = new TreeMap<String, List<ActivityStatisticBean>>();
      
      //init empty-data
      for(int i=0; i< totalDataCoulumn; i++){
        calendar.clear();
        calendar.setTime(fromDate);
        calendar.add(Calendar.MONTH, i);
        String month = calendar.get(Calendar.YEAR) + "-" +partString(calendar.getTime(), "MM") + "-"+ partString(calendar.getTime(), "MMM") + "-" + calendar.get(Calendar.YEAR); //get name of Month
        monthData.put(month, new ArrayList<ActivityStatisticBean>());
      }
      
      for (ActivityStatisticBean bean : list) {
        calendar.clear();    
        calendar.setTime(bean.getCreatedDate());
        String month = calendar.get(Calendar.YEAR) + "-" +partString(calendar.getTime(), "MM") + "-"+ partString(calendar.getTime(), "MMM") + "-" + calendar.get(Calendar.YEAR); //get name of Month
         
        if(monthData.containsKey(month)){
          List<ActivityStatisticBean> listValueOfNode = monthData.get(month);
          listValueOfNode.add(bean);
        }else{
          List<ActivityStatisticBean> listValueOfNode = new ArrayList<ActivityStatisticBean>();
          listValueOfNode.add(bean);
          monthData.put(month, listValueOfNode);
        }
      }
      
      for(String key: monthData.keySet()){
        List<ActivityStatisticBean> listValueOfNode = monthData.get(key);
        Long monthNewUsersValue = 0L;
        Long monthLoginCountValue =0L;
        Long monthForumActiveUsersValue = 0L;
        Long monthNewForumPostsValue = 0L;
        
        Long monthUserConnectionValue = 0L;
        Long monthSocialPostsValue = 0L;
        Long monthEmailNotificationValue = 0L;
        
        for (ActivityStatisticBean obj : listValueOfNode) {
          monthNewUsersValue = monthNewUsersValue + obj.getNewUserToday();
          monthLoginCountValue = monthLoginCountValue + obj.getLoginCountToday();
          monthForumActiveUsersValue = monthForumActiveUsersValue + obj.getForumActiveUserToday();
          monthNewForumPostsValue = monthNewForumPostsValue + obj.getForumPostToday();
          
          monthUserConnectionValue = monthUserConnectionValue + obj.getUserConnectionCountToday();
          monthSocialPostsValue = monthSocialPostsValue + obj.getSocialPostCountToday();
          monthEmailNotificationValue = + monthEmailNotificationValue + obj.getEmailNotificationCountToday(); 
        }
        
        listTitle.add(key.substring(8, key.length()));
        newUsersData.add(monthNewUsersValue);
        loginCountData.add(monthLoginCountValue);
        forumActiveUsersData.add(monthForumActiveUsersValue>0?(Long)(monthForumActiveUsersValue/listValueOfNode.size()):0L);
        newForumPostsData.add(monthNewForumPostsValue);
        
        userConnectionData.add(monthUserConnectionValue);
        socialPostData.add(monthSocialPostsValue);
        emailNotificationData.add(monthEmailNotificationValue);
      }
      chartData.setListTitle(listTitle);
      chartData.setNewUsersData(newUsersData);
      chartData.setLoginCountData(loginCountData);
      chartData.setForumActiveUsersData(forumActiveUsersData);
      chartData.setNewForumPostsData(newForumPostsData);
      
      chartData.setUserConnectionData(userConnectionData);
      chartData.setSocialPostData(socialPostData);
      chartData.setEmailNotificationData(emailNotificationData);
      
      return chartData;
    }
    return null;
  }
  
  private long getTotalPlfDownloadDataFromJson(String json, Date startDate){
    Date today = new Date();
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.setTime(today);
    cal.add(Calendar.DATE, 1);
    Date nexDate = cal.getTime();
    cal.clear();
    if(startDate.after(nexDate)) return 0L; // Do not get data of future
      
    try {
      org.json.simple.JSONObject jsonObj = (org.json.simple.JSONObject) JSONValue.parseWithException(json);
      
      //in case request to get data in future, return 0
      String startDateStr= partString(startDate, "yyyy-MM-dd");
      String returnStartDateStr = jsonObj.get("start_date").toString();
      
      if(returnStartDateStr.substring(0,10).equalsIgnoreCase(startDateStr) == false){
        return 0L;
      }
      
      Long total = Long.parseLong(jsonObj.get("total")+"");
      return total;
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      LOG.error(e.getMessage());
    } catch (Exception e){
      LOG.error(e.getMessage());
    }
    return 0l;
  }
  
  private String httpGet(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    LOG.debug("Calling API: "+ conn.getURL());
    if (conn.getResponseCode() != 200) {
      throw new IOException(conn.getResponseMessage());
    }
    // Buffer the result into a string
    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      sb.append(line);
    }
    rd.close();

    conn.disconnect();
    return sb.toString();
  }
  
  public class CommunityStatistic{
    private long totalMember;
    private long totalPost;
    private long totalDownload;
    private long totalAddon;
    private long totalUserConnection;
    private long totalSocialPost;
    private long totalEmailNotification;
    
    
    public long getTotalUserConnection() {
      return totalUserConnection;
    }
    public void setTotalUserConnection(long totalUserConnection) {
      this.totalUserConnection = totalUserConnection;
    }
    public long getTotalSocialPost() {
      return totalSocialPost;
    }
    public void setTotalSocialPost(long totalSocialPost) {
      this.totalSocialPost = totalSocialPost;
    }
    public long getTotalEmailNotification() {
      return totalEmailNotification;
    }
    public void setTotalEmailNotification(long totalEmailNotification) {
      this.totalEmailNotification = totalEmailNotification;
    }
    public long getTotalMember() {
      return totalMember;
    }
    public void setTotalMember(long totalMember) {
      this.totalMember = totalMember;
    }
    public long getTotalPost() {
      return totalPost;
    }
    public void setTotalPost(long totalPost) {
      this.totalPost = totalPost;
    }
    public long getTotalDownload() {
      return totalDownload;
    }
    public void setTotalDownload(long totalDownload) {
      this.totalDownload = totalDownload;
    }
    public long getTotalAddon() {
      return totalAddon;
    }
    public void setTotalAddon(long totalAddon) {
      this.totalAddon = totalAddon;
    }
    
  }
  
}


