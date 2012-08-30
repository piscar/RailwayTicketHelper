<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bk.railway.servlet.Constants" %>
<%@ page import="com.bk.railway.servlet.RailwayQueryServlet" %>
<%@ page import="com.bk.railway.servlet.RecordStatus" %>
<%@ page import="com.bk.railway.helper.GetInDateProxy" %>
<%@ page import="com.bk.railway.helper.LoginHelper" %>
<%@ page import="com.bk.railway.helper.TaskUtil" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>Railway Helper</title>

<script src="jquery-1.8.0.js" type="text/javascript"></script>    
<script language="javascript">
	
var stationid_to_name = {};

<%
for(Map.Entry<String,String> entry : Constants.STATION_ID_TOMAP.entrySet()) {
%>
stationid_to_name["<%=entry.getKey()%>"]="<%=entry.getValue()%>";
<%
}
%>

function createUpdateTime(eta){

		if(isNaN(eta)) {
				return "";
		}
		else {
				var currentDate = new Date();
		    var etaDay = new Date(eta + currentDate.getTimezoneOffset() * 60000 + 3600000 * 8);
		    return etaDay.toLocaleString();
	  }

}
	
	
function loadStatus(jsonstring) {

		var json = jQuery.parseJSON(jsonstring);
		var routes = json.length;
		for(var r = 0;r < json.length;r++) {
			var updtaeTime = json[r][0].<%=Constants.QUERY_UPDATETIME%>;
			var from_to_string = "";
			var train_no = json[r][0].<%=Constants.TRAIN_NO%>;
		  
		  for(var stop = 0;stop < json[r].length;stop++) {
				from_to_string = from_to_string + "&nbsp;" + stationid_to_name[json[r][stop].<%=Constants.FROM_STATATION%>];
				if(updtaeTime < json[r][stop].<%=Constants.QUERY_UPDATETIME%>) {
					updtaeTime = json[r][stop].<%=Constants.QUERY_UPDATETIME%>;
				}
			}
			
			from_to_string = from_to_string + "&nbsp;" + stationid_to_name[json[r][json[r].length - 1].<%=Constants.TO_STATATION%>];
			$('#qtable tr:last').after("<tr><td>" + json[r][0].<%=Constants.PERSON_ID%> + "</td><td>" + from_to_string + "</td><td>" + json[r][0].<%=RailwayQueryServlet.BORDING_TIME%> + "</td><td>" + json[r][0].<%=Constants.TRAIN_NO%> + "</td><td>" + json[r][0].<%=Constants.ORDER_QTY%> + "</td><td>" + createUpdateTime(updtaeTime) + "</td><td><a href='javascript:submitQuery();'>Query</a></td></tr>");
		}

		//alert("json.person_id=" + json.person_id);
  	//$("#result").html(entities);
  	//

	
}

function submitQuery() {
	var formdata = "<%=Constants.PERSON_ID%>=" + ctno1.elements["<%=Constants.PERSON_ID%>"].value;
	formdata = formdata + "&" + "<%=Constants.FROM_STATATION%>=" + ctno1.elements["<%=Constants.FROM_STATATION%>"].value;
	formdata = formdata + "&" + "<%=Constants.TO_STATATION%>=" + ctno1.elements["<%=Constants.TO_STATATION%>"].value;
	formdata = formdata + "&" + "<%=Constants.GETIN_DATE%>=" + ctno1.elements["<%=Constants.GETIN_DATE%>"].value;
	formdata = formdata + "&" + "<%=Constants.ORDER_QTY%>=" + ctno1.elements["<%=Constants.ORDER_QTY%>"].value;
	formdata = formdata + "&" + "<%=RailwayQueryServlet.NUM_STOP%>=1";
	
	$.ajax({  
  type: "POST",
  url: "/query",
  data: formdata,  
  success: function(data) { 
				loadStatus(data);
       }  
  });  
  
  return false;
	
}


function submitPQuery(){
	var formdata = "<%=Constants.PERSON_ID%>=" + ctno1.elements["<%=Constants.PERSON_ID%>"].value;
	formdata = formdata + "&" + "<%=Constants.FROM_STATATION%>=" + ctno1.elements["<%=Constants.FROM_STATATION%>"].value;
	formdata = formdata + "&" + "<%=Constants.TO_STATATION%>=" + ctno1.elements["<%=Constants.TO_STATATION%>"].value;
	formdata = formdata + "&" + "<%=Constants.GETIN_DATE%>=" + ctno1.elements["<%=Constants.GETIN_DATE%>"].value;
	formdata = formdata + "&" + "<%=Constants.ORDER_QTY%>=" + ctno1.elements["<%=Constants.ORDER_QTY%>"].value;
  //var formdata = $("#ctno1").serialize();

	$.ajax({  
  type: "POST",
  url: "/pquery",
  data: formdata,  
  success: function(data) { 
				loadStatus(data);
       }  
  });  
  
  return false;
	
}
	
</script>    
    
  </head>

  <body>
    <h1>Railway Helper</h1>
    <div id="result">
    	You are <%=LoginHelper.getUsername(request,response)%>	
    </div>
    <table id="qtable">
			<tr><td>Person Id</td><td>Stations</td><td>Date</td><td>Train No</td><td>Quantity</td><td>Update Time</td><td>Link</td></tr>
    </table>
    
<form method="POST" name="ctno1">
<table border="0" cellpadding="5" cellspacing="5">
	<tr>
		<td width="100" class="brown02">Person Id</td>
		<td width="500"><input type="text" name="<%=Constants.PERSON_ID%>"></td>
	</tr>
	<tr>
		<td class="brown02">From</td>
		<td>
			<select name="<%=Constants.FROM_STATATION%>">
<%
for(Map.Entry<String,String> entry : Constants.STATION_ID_TOMAP.entrySet()) {

	if("100" == entry.getKey()) {
%>
				<option selected value="<%=entry.getKey()%>"><%=entry.getKey()%>-<%=entry.getValue()%></option>
<%	
	}
else {

%>
				<option value="<%=entry.getKey()%>"><%=entry.getKey()%>-<%=entry.getValue()%></option>
<%
		}
}
%>				

			</select>
		</td>
	</tr>
	<tr>
		<td class="brown02">To</td>
		<td>
			<select name="<%=Constants.TO_STATATION%>">
<%
for(Map.Entry<String,String> entry : Constants.STATION_ID_TOMAP.entrySet()) {

	if("100" == entry.getKey()) {
%>
				<option selected value="<%=entry.getKey()%>"><%=entry.getKey()%>-<%=entry.getValue()%></option>
<%	
	}
else {

%>
				<option value="<%=entry.getKey()%>"><%=entry.getKey()%>-<%=entry.getValue()%></option>
<%
		}
}
%>
			</select>
		</td>
	</tr>
	<tr>
		<td class="green01">Date</td>
		<td>
			<select name="<%=Constants.GETIN_DATE%>">
				
<%

//final String[] dateStrings = GetInDateProxy.newInstance().getAllBookableDate();
//Arrays.sort(dateStrings,String.CASE_INSENSITIVE_ORDER);
//for(String dateString : dateStrings) {
Calendar cal = Calendar.getInstance(TaskUtil.TIMEZONE);
cal.add(Calendar.DAY_OF_MONTH, 1);
final Date tomorrow = cal.getTime();

for(int plusDay = 0;plusDay < 90;plusDay++) {
%>
			<option value=<%=GetInDateProxy.bookablePlusDay(tomorrow ,plusDay)%>><%=GetInDateProxy.bookableAppendWeek(tomorrow,plusDay)%></option>
<%
}
%>				
			</select>
		</td>
	</tr>
<!--	
	<tr>
		<td class="green01">Train No</td>
		<td>
			<input type="text" name="train_no">
			 <span id="boxDesc" style="display:none;"></span>
		</td>
	</tr>
-->	
	<tr>
		<td class="green01">Quantity</td>
		<td>
			<div id="boxNorm" style="display:block;">
				<select name="<%=Constants.ORDER_QTY%>">
					<option value="1" selected>1</option>
					<option value="2">2</option>
					<option value="3">3</option>
					<option value="4">4</option>
					<option value="5">5</option>
					<option value="6">6</option>
				</select>
			</div>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<p class="orange02">
				<strong>

				</strong>
			</p>
		</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
		<td>
			<!--<button type="submit" style="border:0;background:white;width:100px;">-->
			<a href="javascript:submitPQuery();"><img src="cheak_b.jpeg"></a>
			<a href='javascript:submitQuery();'>Query</a>
			<!--<input type="image" src="cheak_b.jpeg" onClick="">-->
			<!--</button>-->
		</td>
	</tr>
</table>
</form>
    
  </body>
</html>
