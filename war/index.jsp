<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bk.railway.servlet.Constants" %>
<%@ page import="com.bk.railway.servlet.RailwayOrderStatusServlet" %>
<%@ page import="com.bk.railway.servlet.RecordStatus" %>
<%@ page import="com.bk.railway.helper.GetInDateProxy" %>
<%@ page import="com.bk.railway.helper.LoginHelper" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Arrays" %>

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

function createLink(status,record_id){
	
	if(status != "<%=RecordStatus.CANCELED.toString()%>") {
		return "<form action=\"/cancel\" + method=\"post\" name=\"form_cancel_" + record_id + "\"><input type=\"hidden\" name=\"<%=Constants.RECORD_ID%>\" value=\"" + record_id  +"\"><a href=\"javascript: return false\" onclick=\"javascript:document.form_cancel_" + record_id + ".submit();\">Cancel</a></form>";
	}
	else {
		return "";
	}
}

function createEta(entity){
	var status = entity.<%=Constants.RECORD_STATUS%>;
	if("<%=RecordStatus.POSTPONED.toString()%>" == status || "<%=RecordStatus.QUEUE.toString()%>" == status || "<%=RecordStatus.DONE.toString()%>" == status) {
		var eta = parseInt(entity.<%=Constants.ORDER_TASKETA%>);
		if(isNaN(eta)) {
				return "";
		}
		else {
				var currentDate = new Date();
		    var etaDay = new Date(eta + currentDate.getTimezoneOffset() * 60000 + 3600000 * 8);
		    return "&nbsp;next try @ &nbsp;" + etaDay.toLocaleString();
	  }
	}
	else {
		return "";
	}
}
	

function loadStatus() {
	$.post("/status", function(data) {
		var json = jQuery.parseJSON(data);
		var entities = json.<%=RailwayOrderStatusServlet.ENTITY%>;
		for(var i = 0;i < entities.length;i++) {
			$('#statustable tr:last').after("<tr><td>" + entities[i].<%=Constants.PERSON_ID%> + "</td><td>" + stationid_to_name[entities[i].<%=Constants.FROM_STATATION%>] + "</td><td>" + stationid_to_name[entities[i].<%=Constants.TO_STATATION%>] + "</td><td>" + entities[i].<%=Constants.GETIN_DATE%> + "</td><td>" + entities[i].<%=Constants.TRAIN_NO%> + "</td><td>" + entities[i].<%=Constants.ORDER_QTY%>  + "</td><td>" + entities[i].<%=Constants.ORDER_NO%> + "</td><td>" + entities[i].<%=Constants.RECORD_STATUS%> + createEta(entities[i]) + "</td><td>" + createLink(entities[i].<%=Constants.RECORD_STATUS%>,entities[i].<%=Constants.RECORD_ID%>) + "</td></tr>");
		}
		//alert("json.person_id=" + json.person_id);
  	//$("#result").html(entities);
  	//
	});
	
}
	
</script>    
    
  </head>

  <body onLoad="loadStatus()">
    <h1>Railway Helper</h1>
    <div id="result">
    	You are <%=LoginHelper.getUsername(request,response)%>	
    </div>
    <table id="statustable">
			<tr><td>Person Id</td><td>From</td><td>To</td><td>Date</td><td>Train No</td><td>Quantity</td><td>Order No</td><td>Status</td><td>Links</td></tr>
    </table>
    
<form method="POST" action="/send" name="ctno1">
<table border="0" cellpadding="5" cellspacing="5">
	<tr>
		<td width="100" class="brown02">Person Id</td>
		<td width="500"><input type="text" name="person_id"></td>
	</tr>
	<tr>
		<td class="brown02">From</td>
		<td>
			<select name="from_station">
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
			<select name="to_station">
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
			<select name="getin_date">
				
<%
final String[] dateStrings = GetInDateProxy.newInstance().getAllBookableDate();
Arrays.sort(dateStrings,String.CASE_INSENSITIVE_ORDER);
//for(String dateString : dateStrings) {
for(int plusDay = 0;plusDay < 90;plusDay++) {
%>
			<option value=<%=GetInDateProxy.bookablePlusDay(dateStrings[0],plusDay)%>><%=GetInDateProxy.bookableAppendWeek(dateStrings[0],plusDay)%></option>
<%
}
%>				
			</select>
		</td>
	</tr>
	<tr>
		<td class="green01">Train No</td>
		<td>
			<input type="text" name="train_no">
			 <span id="boxDesc" style="display:none;"></span>
		</td>
	</tr>
	<tr>
		<td class="green01">Quantity</td>
		<td>
			<div id="boxNorm" style="display:block;">
				<select name="order_qty_str">
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
			<a href="javascript:return false;" onclick="javascript:document.ctno1.submit();document.ctno1.person_id=''"><img src="oder_a.jpg"></a>
			<!--</button>-->
		</td>
	</tr>
</table>
</form>
    
  </body>
</html>
