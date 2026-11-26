function binaryMessageReceived(message)
{
	var header = message.slice(1,message[0]+1);
	var headerString = new TextDecoder("utf-8").decode(header);
	
	var data = message.slice(message[0]+1,message.length);
	
	console.log(headerString);
	
	
	var s = splitWithTail(headerString,',',2);
	var am = customMessageHandlers[s[0]];
	if (am == null)
	{
		console.log("no listeners available for any channels for custom messages from AM "+s[0]+". BINARY MESSAGE, LENGTH: " + data.length);
		return;
	}
	var methods = am[s[1]];
	if (methods == null)
	{
		console.log("no listener available for channel "+s[1]+" for custom messages from AM "+s[0]+". BINARY MESSAGE, LENGTH: " + data.length);
		return;
	}
	//methods.forEach(function(m){m(s[2]);}); slower aparently
		
	for (var i = 0, len = methods.length; i < len; i++) {
		methods[i](data);
	}
}

function wsConnect()
{
	if (typeof topAppId === "undefined")
	{
		return;
	}
	
	
	
	
	var loc = window.location, new_uri, port;

	if (typeof webSocketPortOverride === "undefined")
	{
		if (loc.protocol === "https:") {
		    new_uri = "wss:";
		    port = 443;
		} else {
		    new_uri = "ws:";
		    port = 80;
		}
	}
	else
	{
		new_uri = "ws:";
		port = webSocketPortOverride;
	}
	

	new_uri += "//" + loc.host + ":"+port;
	new_uri += loc.pathname + "SessionSocket";
	
	ws = new WebSocket(new_uri);

    		            ws.onopen = function () {
    		            	console.log("websocket connected");
    		                ws.send("CONNECTED,"+topAppId+","+getCookie("PHPSESSID"));
    		            };
    		            ws.onmessage = function (event) {
    		            	if (event.data instanceof Blob)
    		            	{
    		            		var arrayBuffer;
    		            		var fileReader = new FileReader();
    		            		fileReader.onload = function(event2) {
    		            		    binaryMessageReceived(new Uint8Array(event2.target.result));

    		            		};
    		            		fileReader.readAsArrayBuffer(event.data);
    		            	}
    		            	else if (event.data instanceof ArrayBuffer)
    		            	{
    		            		binaryMessageReceived(new Uint8Array(event.data));
    		            	}
    		            	else if (event.data == "ping")
    		            	{
    		            		ws.send("PING,"+topAppId+","+getCookie("PHPSESSID"));
    		            	}
    		            	else if(event.data == "FORCE_REFRESH")
    		            	{
    		            		window.location.reload();
    		            	}
    		            	else
    		            	{
    		            		response(event.data,null);
    		            	}
    		            };
    		            ws.onclose = function (event) {
    		                setTimeout(wsConnect,1000);
    		                
    		            };
    		        	ws.onerror = function(error) {
    		      		};

}

function wsAmMessage(amId,message,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	if (typeof ws === 'undefined')
	{
		return false;
	}
	try
	{
		ws.send("AM,"+topAppId+","+getCookie("PHPSESSID")+","+amId+","+c+","+message);
	}
	catch(err)
	{
		//console.log('error sending, recreating websocket connection');
		//ws.close();
		//wsConnect();
		return false;
	}
	return true;
}

function wsAmBinaryMessage(amId,message,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	if (typeof ws === 'undefined')
	{
		return false;
	}
	try
	{
		//ws.send("AM,"+topAppId+","+getCookie("PHPSESSID")+","+amId+","+c+","+message);
		if (!("TextEncoder" in window)) 
			  alert("Sorry, binary messages wont work, neccesary functions not supported in this browser");

		var enc = new TextEncoder(); // always utf-8
		var header = enc.encode("AM,"+topAppId+","+getCookie("PHPSESSID")+","+amId+","+c);
		
		var combined = new Uint8Array(1 + header.length + message.length);
		combined[0] = header.length;
		combined.set(header, 1);
		combined.set(message,1 + header.length);
		

		ws.send(combined);
	}
	catch(err)
	{
		return false;
	}
	return true;
}


function getCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1);
        if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
    }
    return "";
}

function setCookie(name,value,time) {
    var expires = "";
    if (time) {
        var date = new Date();
        date.setTime(time);
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

 var deviceId = getCookie("DEVICE_ID");
 if (deviceId == "")
 {
   deviceId = uuidv4();
   setCookie("DEVICE_ID",deviceId,2114427600000);
 }

function refreshListeners(com)
{
	var dateFields = com.find(".DateField").addBack(".DateField");
	var suffixLen = "_field".length;
	for (var i = 0; i < dateFields.length; i++) 
	{
		
	    var picker = new Pikaday(
	    	    {
	    	        field: dateFields[i],
	    	        trigger : $("#"+dateFields[i].id.substr(0,dateFields[i].id.length-suffixLen)+"_calendarbutton")[0],
	    	        firstDay: 1,
	    	       // minDate: new Date(2000, 0, 1),
	    	       // maxDate: new Date(2020, 12, 31),
	    	       // yearRange: [2000,2020],
	    	        yearRange: 9,
	    			format : 'DD-MMM-YYYY',
	    	    });
	    
	}
	
	for (var c in eventHandlers) 
	{
		if (eventHandlers.hasOwnProperty(c)) 
		{
			var coms = com.find(c).addBack(c);
			if (coms.length > 0)
			{
				for (var e in eventHandlers[c]) 
				{
					if (eventHandlers[c].hasOwnProperty(e)) 
					{	
						coms.on(e,eventHandlers[c][e]);
					}
				}
			}
		}
	}	
	
	//setTimeout(function(){ resizeTables(com); }, 10);
	window.requestAnimationFrame(function(){ resizeTables(com); });
	
}

function responseAndResizeTable(tableId)
{;
	return function(data,status)
	{
		response(data,status);
		resizeTable($('#'+tableId));
	}
}

function responseAndEnable(enable)
{
	return function(data,status)
	{
		$('#'+enable).attr("disabled",false);
		response(data,status);
	}
}

function response(data,status)
{
	var seperator = data.indexOf("-");
	var s = data.substring(0,seperator);
	var d = data.substring(seperator+1,data.length);
	
	
	
	s = s.split(",");
	var size = s[0];
	var sizes = s.slice(1,size+1);
	for (var i = 0; i < sizes.length;i++)
	{
		var message = d.substring(0,sizes[i]);
		var sep = message.indexOf(",");
		processMessage(message.substring(0,sep),message.substring(sep+1,data.length));
		d = d.substring(sizes[i],d.length);
	}	
	$("body").removeClass("wait");
	if (futureFocus != null)
	{
		$('#'+futureFocus).focus();
		futureFocus = null;
	}
}

function setattr(s0,s1,s2)
{
	if (s1 == 'class')
	{
		var oldClassName = '.'+$("#"+s0).prop('class');	
		for (var key in eventHandlers[oldClassName]) 
		{
			if (eventHandlers[oldClassName].hasOwnProperty(key)) 
			{
				$("#"+s0).off(key);
			}
		}
	}
	$("#"+s0).attr(s1,s2);
	if (s1 == 'class')
	{
		for (var key in eventHandlers['.'+s2]) 
		{
			if (eventHandlers['.'+s2].hasOwnProperty(key)) 
			{
				$("#"+s0).on(key,eventHandlers['.'+s2][key]);
			}
		}
	}
}

function splitWithTail(value, separator, limit) {
    var pattern, startIndex, m, parts = [];

    if(!limit) {
        return value.split(separator);
    }

    if(separator instanceof RegExp) {
        pattern = new RegExp(separator.source, 'g' + (separator.ignoreCase ? 'i' : '') + (separator.multiline ? 'm' : ''));
    } else {
        pattern = new RegExp(separator.replace(/([.*+?^${}()|\[\]\/\\])/g, '\\$1'), 'g');
    }

    do {
        startIndex = pattern.lastIndex;
        if(m = pattern.exec(value)) {
            parts.push(value.substr(startIndex, m.index - startIndex));
        }
    } while(m && parts.length < limit - 1);
    parts.push(value.substr(pattern.lastIndex));

    return parts;
}

function decimalFormat(com,amount)
{
	amount = parseInt(amount);
	var pos = com.value.length-com.selectionStart
	var l = com.value.length;
	numberString = com.value.replace(/,/g, '');
	if (numberString.includes('.'))
	{
		var v = numberString.split('.');
		com.value = decimalFormatInternal(v[0],amount)+"."+v[1];
	}
	else
	{
		com.value = decimalFormatInternal(numberString,amount);
	}
	//if (com.value.charAt(pos-1) == ","){pos++};
	com.setSelectionRange(com.value.length-pos,com.value.length-pos);
}

function decimalFormatInternal(numberString,amount)
{
	var count = 0;
	for (var i = numberString.length-1;i > 0;i--)
	{
		count++;
		if (count == amount)
		{
			count = 0;
			numberString = numberString.substr(0, i) + "," + numberString.substr(i);
		}
	}
	return numberString;
}

function resizeTables(com)
{
	$.each(com.find(".table_frozen").addBack(".table_frozen"), function( i, table ) 
	{		
		resizeTable(table);
	});
}
function resizeTable(table)
{
	/*
	var width = $(table).width();
	var oldDisplay = table.style.display;
	table.style.display = 'none';
	var width = Math.min(width,$(table).parent().width());
	table.style.display = oldDisplay;
	*/

	var width = $(table).width();
	var heightPlaceHolder = document.createElement('div');
	table.parentNode.insertBefore(heightPlaceHolder, table);
	heightPlaceHolder.style.height = $(table).height();
	var oldDisplay = table.style.display;
	table.style.display = 'none';
	var width = Math.min(width,$(table).parent().width());
	heightPlaceHolder.parentNode.removeChild(heightPlaceHolder);
	table.style.display = oldDisplay;

	
	width = width - 2;//prevents extra horizontal scrollbar from forming in certian setups. Not sure why. 
	var totalwidth = $(table).attr("totalwidth");
	var leftwidth = $(table).attr("leftwidth");
	totalwidth = Math.min(width,totalwidth);
	var newRightwidth = Math.max($(table).attr("minrightwidth"),totalwidth-leftwidth);
	var navLength = parseInt(leftwidth) + parseInt(newRightwidth);
	newRightwidth = newRightwidth - $(table).attr("paddingRight");
	$(table).find(".table_frozen_row").css({"width":newRightwidth});
	$(table).find(".nav").css({"width":navLength});


	if (frozenTableScroll != -1 && table.className == 'table_frozen')
	{
		table.childNodes.item(1).childNodes.item(1).scrollLeft = frozenTableScroll;
		frozenTableScroll = -1;
	}
}

function download(link)
{
	document.getElementById('download_iframe').src = link;
}

var frozenTableScroll = -1;
var test = false;
function processMessage(type,message)
{
	if (type == 'setdiv')
	{
		var seperator = message.indexOf("-");
		//var id = "#"+message.substring(0,seperator);
		var id = message.substring(0,seperator);
		var el = document.getElementById(id);
		if (el.className == 'table_frozen')
		{
			if (el.childNodes.item(1) != null 
					&& el.childNodes.item(1).childNodes.item(1) != null 
					&& el.childNodes.item(1).childNodes.item(1).scrollLeft != null)
			frozenTableScroll = el.childNodes.item(1).childNodes.item(1).scrollLeft;
		}
		var value = message.substring(seperator+1,message.length);
		var newCom = $(value);
		$("#"+id).replaceWith(newCom);
		refreshListeners(newCom);
	}
	if (type == 'setparentdiv')
	{
		var seperator = message.indexOf("-");
		var id = "#"+message.substring(0,seperator);
		var value = message.substring(seperator+1,message.length);
		var newCom = $(value);
		$(id).parent().replaceWith(newCom);
		refreshListeners(newCom);
	}
	if (type == 'append')
	{
		var seperator = message.indexOf("-");
		var id = "#"+message.substring(0,seperator);
		var value = message.substring(seperator+1,message.length);
		var newCom = $(value);
		$(id).append(newCom);
		refreshListeners(newCom);
	}
	if (type == 'appendtofirstchild')
	{
		var seperator = message.indexOf("-");
		var id = "#"+message.substring(0,seperator);
		var value = message.substring(seperator+1,message.length);
		var newCom = $(value);
		$(id).children().first().append(newCom);
		refreshListeners(newCom);
	}
	else if (type == 'setprop')
	{
		//var s = message.split(',');
		var s = splitWithTail(message,',',3);

		setTimeout(function() 
		{ 
			if (s[1] == 'class')
			{
				var oldClassName = '.'+$("#"+s[0]).prop('class');	
				for (var key in eventHandlers[oldClassName]) 
				{
					if (eventHandlers[oldClassName].hasOwnProperty(key)) 
					{
						$("#"+s[0]).off(key);
					}
				}
			}
			$("#"+s[0]).prop(s[1],s[2]);
			if (s[1] == 'class')
			{
				for (var key in eventHandlers['.'+s[2]]) 
				{
					if (eventHandlers['.'+s[2]].hasOwnProperty(key)) 
					{
						$("#"+s[0]).on(key,eventHandlers['.'+s[2]][key]);
					}
				}
			}
		}, 0);
	}
	else if (type == 'setattr')
	{
		//var s = message.split(',');
		var s = splitWithTail(message,',',3);
		setattr(s[0],s[1],s[2]);
	}
	else if (type == 'displaymsg')
	{
		if (!$('#sk_message').length)       
		{
			var markup = '	<div id="sk_message" class="modalDialog">\
								<div>\
									<table id ="sk_message_list"></table>\
									<button id="sk_message_ok_button" type="submit" class="sk_message_ok_button" value="create">OK</button>\
								</div>\
							</div>';

			$(document.body).append(markup);
			setTimeout(function() { 
				$("#sk_message").prop("class","modalDialog modalDialog-open");
			}, 0);
			
			$("#sk_message_ok_button").on('click',function(){
																$("#sk_message").remove();
																$previousFocus.focus();
															});
			$("#sk_message_ok_button").on('focusout',function(){
																$("#sk_message_ok_button").focus();
															});
			$previousFocus = $(':focus');
			$("#sk_message_ok_button").focus();
		}

		var seperator = message.indexOf(",");
		var type =  message.substring(0,seperator);
		var value = message.substring(seperator+1,message.length);
		if (['info', 'warn', 'error', 'fatal'].indexOf(type) < 0)
			type = 'error';

		// The message is text, never markup; a line break in it starts a new line.
		var row = $('<tr>');
		$('<td>').append($('<img>', { src: 'skylin/images/' + type + '.png', height: 16, width: 16, alt: type })).appendTo(row);
		$('<td>').css({ 'max-width': '380px', 'word-wrap': 'break-word', 'white-space': 'pre-line' })
			.text('\u00a0' + value).appendTo(row);
		$("#sk_message_list").append(row);
	}
	else if (type == 'openwindow')
	{
		var nameAt = message.lastIndexOf('||');
		if (nameAt >= 0)
		{
			var url = message.substring(0, nameAt);
			// A window name cannot contain spaces. Chrome opens nothing when it does.
			var name = message.substring(nameAt + 2).replace(/\s+/g, '_');
			var opened = null;
			try { opened = window.open(url, name); } catch (e) { opened = null; }
			if (!opened)
				window.open(url);
		}
		else
		{
			window.open(message);
		}
	}
	else if (type == 'locationwindow')
	{
		window.location.assign(message);
	}
	else if (type == 'settitle')
	{
		document.title = message;
	}
	else if (type == 'click')
	{
		$("#"+message).click();
	}
	else if (type == 'reload')
	{
		window.location.reload();
	}
	else if (type == 'download')
	{
		download(message);
	}
	else if (type == 'focus')
	{
		var myElem = document.getElementById(message);
		if (myElem === null){
			futureFocus = message;
		}
		else
		{
			$('#'+message).focus();
			futureFocus = null;
		}
	}
	else if (type == 'seturl')
	{
		history.pushState({}, null, message);
	}
	else if (type == 'custom')
	{
		var s = splitWithTail(message,',',3);
		var am = customMessageHandlers[s[0]];
		if (am == null)
		{
			console.log("no listeners available for any channels for custom messages from AM "+s[0]+". MESSAGE: " + s[2]);
			return;
		}
		var methods = am[s[1]];
		if (methods == null)
		{
			console.log("no listener available for channel "+s[1]+" for custom messages from AM "+s[0]+". MESSAGE: " + s[2]);
			return;
		}
		//methods.forEach(function(m){m(s[2]);}); slower aparently
			
		for (var i = 0, len = methods.length; i < len; i++) {
  			methods[i](s[2]);
		}
	}
	else if (type == 'scrolltobottom')
	{
		var myElem = document.getElementById(message);
		if (myElem !== null){
			myElem.scrollTop = myElem.scrollHeight;
		}
	}
	else if (type == 'setCookie')
	{
		var s = splitWithTail(message,';',3);
		setCookie(s[0],s[1],s[2],'/');
	}
	else if (type == 'postmessage')
	{
		var s = splitWithTail(message,',',2);
		var frame = document.getElementById(s[0]).firstElementChild;
		frame.contentWindow.postMessage(s[1],'*');
	}
	else if (type == 'requestFullscreen')
	{
		var myElem = document.getElementById(message);
		if (myElem !== null){
			myElem.requestFullscreen();
		}
	}
	else if (type == 'toggleFullscreen')
	{
		var myElem = document.getElementById(message);

		if (myElem !== null){
			if (document.fullscreenElement == myElem)
			{
				document.exitFullscreen();
			}
			else
			{
				myElem.requestFullscreen();
			}
		}
	}
}

function strEndsWith(str, suffix) {
    return str.match(suffix+"$")==suffix;
}

customMessageHandlers = {};
function registerToMessageChannel(amId,func,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	
	if (typeof customMessageHandlers[amId] === 'undefined')
	{
		customMessageHandlers[amId] = {};
	}
	if (typeof customMessageHandlers[amId][c] === 'undefined')
	{
		customMessageHandlers[amId][c] = [];
	}
	customMessageHandlers[amId][c].push(func);
}

function deregisterFromMessageChannel(amId,func,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	if (typeof customMessageHandlers[amId] === 'undefined')
	{
		return;
	}
	if (typeof customMessageHandlers[amId][c] === 'undefined')
	{
		return;
	}
	
	var index = customMessageHandlers[amId][c].indexOf(func);
	if (index > -1) 
	{
	    customMessageHandlers[amId][c].splice(index, 1);
	}
}

function deregisterAllFromMessageChannel(amId,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	if (typeof customMessageHandlers[amId] === 'undefined')
	{
		return;
	}
	if (typeof customMessageHandlers[amId][c] === 'undefined')
	{
		return;
	}
	
	delete customMessageHandlers[amId][c];
}


function messageAM(amId,message,channel)
{
	var c;
	if (typeof channel === 'undefined')
	{
		c = 'DEFAULT';
	}	
	else 
	{
		c = channel;
	}
	
	var m = {"amId":amId,"channel":c,"message":message};
	
	var xhr = new XMLHttpRequest();
	xhr.open("POST", "restless.php?com.distantmelody.framework.skylin.services.builtin.MessageAM", true);
	xhr.setRequestHeader('Content-Type', 'text/plain');
	xhr.send(JSON.stringify(m));
}

var ctrlDown = false;
$(document).keydown(function(e)
	    {
	        if (e.keyCode == 17) ctrlDown = true;
	    }).keyup(function(e)
	    {
	        if (e.keyCode == 17) ctrlDown = false;
	    });

var hoverInfoWidth;
var futureFocus;





var ajaxurl = 'skylin/Response.php',
data =  {'flush': ''};
$.post(ajaxurl, data,response);


$(document).on("keydown", function (e) {//prevents backspace back navigation
	if (e.which === 8 && !$(e.target).is("input:not([readonly]):not([type=radio]):not([type=checkbox]), textarea, [contentEditable], [contentEditable=true]")) {
		e.preventDefault();
	}
});



eventHandlers = {};
eventHandlers['.button'] = {};
eventHandlers['.button']['click'] = function()
{
	if ($(this).attr("action_name") !== 'undefined')
	{
		if ($('[block_action="'+$(this).attr("action_name")+'"]').length > 0)
		{
			return;
		}
	}
	$("body").addClass("wait");
	$(this).attr("disabled",true);
    var ajaxurl = 'skylin/Button.php',
    data =  {'click': $(this).attr("id")};
	$.post(ajaxurl, data,responseAndEnable($(this).attr("id")));
};

eventHandlers['.group'] = {};
eventHandlers['.group']['click'] = function()
{
	var clickable = $(this).attr('clickable');
	if (typeof clickable !== typeof undefined && clickable !== false) 
	{
		if ($(this).attr("action_name") !== 'undefined')
		{
			if ($('[block_action="'+$(this).attr("action_name")+'"]').length > 0)
			{
				return;
			}
		}
        var ajaxurl = 'skylin/Group.php',
        data =  {'clickGroup': $(this).attr("id")};
        $.post(ajaxurl, data,response);
	}
};

eventHandlers['.button_image'] = {};
eventHandlers['.button_image']['click'] = eventHandlers['.button']['click']

eventHandlers['.LinkTextField'] = {};
eventHandlers['.LinkTextField']['click'] = function()
{
	if ($(this).attr("action_name") !== 'undefined')
	{
		if ($('[block_action="'+$(this).attr("action_name")+'"]').length > 0)
		{
			return;
		}
	}
	$("body").addClass("wait");
	$(this).attr("disabled",true);
    var ajaxurl = 'skylin/TextField.php',
    data =  {'linkClick': $(this).attr("id")};
	$.post(ajaxurl, data,responseAndEnable($(this).attr("id")));
};	

eventHandlers['.search_lov_button'] = {};
eventHandlers['.search_lov_button']['click'] = function()
{
	if ($(this).attr("action_name") !== 'undefined')
	{
		if ($('[block_action="'+$(this).attr("action_name")+'"]').length > 0)
		{
			return;
		}
	}
	$("body").addClass("wait");
	$(this).attr("disabled",true);
    var ajaxurl = 'skylin/SearchLov.php',
    data =  {'searchLovClick': $(this).attr("id")};
	$.post(ajaxurl, data,responseAndEnable($(this).attr("id")));
};	

eventHandlers['.TextField'] = {};
eventHandlers['.TextField']['change'] = function(event)
{
	var value = $(this).val();
	if ($(this).attr("sk_type") == 'number')
	{
		value = value.replace(/,/g, '');
		if ($(this).attr("place_value_commas") > 0)
		{
			var com = this;
			decimalFormat(com,$(this).attr("place_value_commas"));
		}
	}
    var ajaxurl = 'skylin/TextField.php',
    data =  {'change': $(this).attr("id")+","+value};
    $(this).parent().prop('class','TextField_tip_disabled');
    $.post(ajaxurl, data,response);
};
eventHandlers['.TextField']['mousedown'] = function(event)
{
	if (event.target.getAttribute("draggablecontainer") != null)
	{
		document.getElementById(event.target.getAttribute("draggablecontainer")).draggable = false;
	}
}
eventHandlers['.TextField']['mouseup'] = function(event)
{
	if (event.target.getAttribute("draggablecontainer") != null)
	{
		document.getElementById(event.target.getAttribute("draggablecontainer")).draggable = true;
	}
}
eventHandlers['.TextField']['mouseleave'] = eventHandlers['.TextField']['mouseup'];


eventHandlers['.TextFieldMulti'] = {};
eventHandlers['.TextFieldMulti']['change'] = function(event)
{
    var ajaxurl = 'skylin/TextField.php',
    data =  {'change': $(this).attr("id")+","+$(this).val()};
    $(this).parent().prop('class','TextField_tip_disabled');
    $.post(ajaxurl, data,response);
};

eventHandlers['.DateField'] = {};
eventHandlers['.DateField']['change'] = function(event)
{	
	var ajaxurl = 'skylin/Date.php',
    data =  {'changeDate': $(this).parent().parent().attr("id")+","+$(this).val()};
	$(this).parent().prop('class','TextField_tip_disabled');
    $.post(ajaxurl, data,response);
};

eventHandlers['.TextField']['keydown'] = function(e)
{
	if (e.keyCode == 13) 
	{
		var value = $(this).val();
		if ($(this).attr("sk_type") == 'number')
		{
			value = value.replace(/,/g, '');
			if ($(this).attr("place_value_commas") > 0)
			{
				var com = this;
				decimalFormat(com,$(this).attr("place_value_commas"));
			}
		}
		var ajaxurl = 'skylin/TextField.php',
        data =  {'change': $(this).attr("id")+","+value,
        		 'linkClick': $(this).attr("id")};
        $.post(ajaxurl, data,response);
		var ajaxurl = 'skylin/TextField.php';
		e.preventDefault();
		e.stopPropagation();//perhaps add this in more places.
	}
	if ($(this).attr("sk_type") == 'number')
	{
		if (!ctrlDown && e.keyCode != 17 && $(this).attr("place_value_commas") > 0)
		{
			var com = this;
			var p = $(this).attr("place_value_commas");
			setTimeout(function(){ decimalFormat(com,p);}, 0);
		}
		// Allow: backspace, delete, tab, escape, enter - and .
		if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 110, 189, 190, 109]) !== -1 ||
			 // Allow: Ctrl+A
			(e.keyCode == 65 && e.ctrlKey === true) || 
			 // Allow: home, end, left, right, down, up
			(e.keyCode >= 35 && e.keyCode <= 40)) {
				 // let it happen, don't do anything
				 return;
		}
		//var ctrlKey = 17, vKey = 86, cKey = 67;
		// Ensure that it is a number and stop the keypress
		if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105) && e.keyCode != 17 && e.keyCode != 86 && e.keyCode != 67) {
			e.preventDefault();
		}
		if ((e.keyCode == 86 || e.keyCode == 67) && !ctrlDown)
		{
			e.preventDefault();
		}
	}	
};
eventHandlers['.TextField']['keyup'] = function(e)
{
	if (e.keyCode == 13) 
	{
		e.stopPropagation();
	}
}

eventHandlers['.table-row-hover'] = {};
eventHandlers['.table-row-hover']['mouseleave'] = function(event)
{
	id = $(this).attr("id");
	if (id)
	{
		suffix = "_right";
		if (id.indexOf(suffix, id.length - suffix.length) !== -1)
		{
			id = id.substring(0,id.length-suffix.length);
		}
		setattr(id,'class','table-row');
		setattr(id+"_right",'class','table-row');
	}
};

eventHandlers['.table-row-hover']['click'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).attr("id");
	suffix = "_right";
	if (id.indexOf(suffix, id.length - suffix.length) !== -1)
	{
		id = id.substring(0,id.length-suffix.length);
	}
	data =  {'selectRow': id};
	$.post(ajaxurl, data,response);
};

eventHandlers['.table-row'] = {};
eventHandlers['.table-row']['mouseover'] = function(event)
{
	id = $(this).attr("id");
	if (id)
	{
		suffix = "_right";
		if (id.indexOf(suffix, id.length - suffix.length) !== -1)
		{
			id = id.substring(0,id.length-suffix.length);
		}
		setattr(id,'class','table-row-hover');
		setattr(id+"_right",'class','table-row-hover');
	}
};
var dragDataTransferData;//becuz broken html drag spec...
eventHandlers['.table-row']['dragstart'] = function(ev)
{
	var a = this.id.split("x"); 
	var table = "x"+a[1]+"x"+a[2];
	//ev.originalEvent.dataTransfer.setData("text", "table-row,"+table+","+ev.target.id);
	//dragDataTransferData = ev.originalEvent.dataTransfer.getData("text");
	
	ev.originalEvent.dataTransfer.setData("text", "bogus data");//so dragging works in firefox.
	
	dragDataTransferData = "table-row,"+table+","+ev.target.id;
}

eventHandlers['.table-row']['dragover'] = function(ev)
{
	if (ev.target instanceof HTMLInputElement)
	{
		ev.preventDefault();
	}
	//var data = ev.originalEvent.dataTransfer.getData("text").split(",");
	var data = dragDataTransferData.split(",");
	if (data[0] != 'table-row')return;
	
	var a = this.id.split("x"); 
	var table = "x"+a[1]+"x"+a[2];
	if (data[1] != table) return;	
	
	$(this).addClass("table-row_drag-over");
	var id = $(this).attr("id");
	//if (id.endsWith("_right"))
	if (strEndsWith(id,"_right"))
	{
		id = id.substring(0,id.length-6); 
	}
	else
	{
		id = id + "_right";
	}
	$('#'+id).addClass("table-row_drag-over");
	
	
	ev.preventDefault();
}

eventHandlers['.table-row']['dragleave'] = function(event)
{
	$(this).removeClass("table-row_drag-over");
	var id = $(this).attr("id");
	//if (id.endsWith("_right"))
	if (strEndsWith(id,"_right"))
	{
		id = id.substring(0,id.length-6); 
	}
	else
	{
		id = id + "_right";
	}
	$('#'+id).removeClass("table-row_drag-over");
}

eventHandlers['.table-row']['drop'] = function(ev)
{
	//var data = ev.originalEvent.dataTransfer.getData("text").split(",");
	var data = dragDataTransferData.split(",");
	if (data[0] != 'table-row')return;
	
	var a = this.id.split("x"); 
	var table = "x"+a[1]+"x"+a[2];
	if (data[1] != table) return;	
	
	var a = data[2];
	var b = this.id;
	
	//if (a.endsWith("_right"))
	if (strEndsWith(a,"_right")) a = a.substring(0,a.length-6);
	//if (b.endsWith("_right"))
	if (strEndsWith(b,"_right")) b = b.substring(0,b.length-6);
	
    var ajaxurl = 'skylin/Table.php',
    data =  {'dragRow': a+","+b};
    $.post(ajaxurl, data,response);
}

eventHandlers['.table-row_selected'] = {};
eventHandlers['.table-row_selected']['dragstart'] = eventHandlers['.table-row']['dragstart'];
eventHandlers['.table-row_selected']['dragover'] = eventHandlers['.table-row']['dragover'];
eventHandlers['.table-row_selected']['dragleave'] = eventHandlers['.table-row']['dragleave'];
eventHandlers['.table-row_selected']['drop'] = eventHandlers['.table-row']['drop'];
eventHandlers['.table-row-hover']['dragstart'] = eventHandlers['.table-row']['dragstart'];
eventHandlers['.table-row-hover']['dragover'] = eventHandlers['.table-row']['dragover'];
eventHandlers['.table-row-hover']['dragleave'] = eventHandlers['.table-row']['dragleave'];
eventHandlers['.table-row-hover']['drop'] = eventHandlers['.table-row']['drop'];

eventHandlers['.page_control_item'] = {};
eventHandlers['.page_control_item']['dragover'] = function(ev)
{
	if (ev.target instanceof HTMLInputElement)
	{
		ev.preventDefault();
	}
	var data = dragDataTransferData.split(",");
	if (data[0] != 'table-row')return;
	
	var a = this.id.split("x"); 
	var table = "x"+a[1]+"x"+a[2];
	if (data[1] != table) return;	
	
	$(this).addClass("table-row_drag-over");
	
	
	ev.preventDefault();
}
eventHandlers['.page_control_item']['dragleave'] = function(event)
{
	$(this).removeClass("table-row_drag-over");
}
eventHandlers['.page_control_item']['drop'] = function(ev)
{
	var data = dragDataTransferData.split(",");
	if (data[0] != 'table-row')return;
	
	var a = this.id.split("x"); 
	var table = "x"+a[1]+"x"+a[2];
	if (data[1] != table) return;	
	
	var b = a[4].substring(4);
	var a = data[2];

	
	if (strEndsWith(a,"_right")) a = a.substring(0,a.length-6);
	
	
    var ajaxurl = 'skylin/Table.php',
    data =  {'dragRowToPage': a+","+b};
    $.post(ajaxurl, data,response);
}

eventHandlers['.table-row_toggle'] = {};
eventHandlers['.table-row_toggle']['click'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).attr("id");
	id = id.substring(0,id.length-7);
	data =  {'toggleRow': id};
	$.post(ajaxurl, data,response);
};

eventHandlers['.heading-sort-asc'] = {};
eventHandlers['.heading-sort-asc']['mousedown'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).parent().parent().attr("id");
	console.log(id);
	data =  {'sortCol': id,
			'dir':'asc'};
	$.post(ajaxurl, data,response);
	event.preventDefault();
};

eventHandlers['.heading-sort-desc'] = {};
eventHandlers['.heading-sort-desc']['mousedown'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).parent().parent().attr("id");
	console.log(id);
	data =  {'sortCol': id,
			'dir':'desc'};
	$.post(ajaxurl, data,response);
	event.preventDefault();
};

eventHandlers['.heading-sort-asc-selected'] = {};
eventHandlers['.heading-sort-asc-selected']['mousedown'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).parent().parent().attr("id");
	console.log(id);
	data =  {'sortCol': id,
			'dir':'nosort'};
	$.post(ajaxurl, data,response);
	event.preventDefault();
};
eventHandlers['.heading-sort-desc-selected'] = {};
eventHandlers['.heading-sort-desc-selected']['mousedown'] = eventHandlers['.heading-sort-asc-selected']['mousedown'];

eventHandlers['.heading-col'] = {};
eventHandlers['.heading-col']['mousedown'] = function(event)
{
	var ajaxurl = 'skylin/Table.php';
	id = $(this).attr("id");
	data =  {'selectCol': id};
	$.post(ajaxurl, data,response);
};


eventHandlers['.heading-col']['dragstart'] = function(ev)
{
	var table = "todo";
	dragDataTransferData = "table-col,"+table+","+this.id;
}
eventHandlers['.heading-col']['dragover'] = function(ev)
{
	if (ev.target instanceof HTMLInputElement)
	{
		ev.preventDefault();
	}
	//var data = ev.originalEvent.dataTransfer.getData("text").split(",");
	var data = dragDataTransferData.split(",");
	if (data[0] != 'table-col')return;
	
	var table = 'todo';
	if (data[1] != table) return;	
	
	$(this).addClass("table-row_drag-over");
	
	ev.preventDefault();
}

eventHandlers['.heading-col']['dragleave'] = function(ev)
{
	$(this).removeClass("table-row_drag-over");
}

eventHandlers['.heading-col']['drop'] = function(ev)
{
	$(this).removeClass("table-row_drag-over");
	
	var data = dragDataTransferData.split(",");
	
    var ajaxurl = 'skylin/Table.php',
    data =  {'dragCol': data[2]+","+this.id};
    $.post(ajaxurl, data,response);
}

eventHandlers['.grid_next_button'] = {};
eventHandlers['.grid_next_button']['click'] = function()
{
	$("body").addClass("wait");
    var id = $(this).attr("id");
    var ajaxurl = 'skylin/Table.php',
    data =  {'next': id};
    $.post(ajaxurl, data,responseAndResizeTable(id.substr(0,id.length-5)));
    //resizeTable($('#'+id.substr(0,id.length-5)));
	//setTimeout(function(){resizeTable($('#'+id.substr(0,id.length-5))); }, 5000);
};	
eventHandlers['.grid_back_button'] = {};
eventHandlers['.grid_back_button']['click'] = function()
{
	$("body").addClass("wait");
    var id = $(this).attr("id");
    var ajaxurl = 'skylin/Table.php',
    data =  {'back': id};
    $.post(ajaxurl, data,responseAndResizeTable(id.substr(0,id.length-5)));
    //resizeTable($('#'+id.substr(0,id.length-5)));
	//setTimeout(function(){resizeTable($('#'+id.substr(0,id.length-5))); }, 5000);
};	

eventHandlers['.table_frozen'] = {};
eventHandlers['.table_frozen']['resize'] = function(event)
{
	/*
	console.log("resizing");
	var width = $(this).width();
	var totalwidth = $(this).attr("totalwidth");
	var leftwidth = $(this).attr("leftwidth");
	totalwidth = Math.min(width,totalwidth);
	var newRightwidth = totalwidth-leftwidth;
	$(this).find(".table_frozen_row").css({"width":newRightwidth});
	*/
}

eventHandlers['.table-col'] = {};
eventHandlers['.table-col']['scroll'] = function(event)
{
	this.parentElement.parentElement.parentElement.children.item(1).children.item(0).children.item(1).scrollTop = this.scrollTop;
}

eventHandlers['.RoTextField'] = {};
eventHandlers['.RoTextField']['mouseover'] = function(event)
{
	if ($(this).outerWidth() < $(this)[0].scrollWidth)
	{
		hoverInfoWidth = $(this).width();
		hoverInfoHeight = $(this).height();
		$('#hover_info').html($(this).html());
		$('#hover_info').show();
	}
};
eventHandlers['.RoTextField']['mouseleave'] = function(event)
{
	//alert(2);
	$('#hover_info').html('');
	$('#hover_info').hide();
}
eventHandlers['.tab'] = {};
eventHandlers['.tab']['click'] = function(event)
{
    var ajaxurl = 'skylin/TabStrip.php',
    data =  {'changetab': $(this).attr("id")};
    $.post(ajaxurl, data,response);
    resizeTables($(this).parent());
    
    //This is a hack to work around an issue in chrome regarding iframes and scrollbars
	var $d = $(this).parent().children().last();
    //A tab that is not rendered yet, or has shallow content, has no element three levels down.
    var inner = $d.children().first().children().first().children().first()[0];
    if (inner && inner.tagName == 'IFRAME')
    {
    	$d[0].style.zoom = 1.03;
    	setTimeout(function(){$d[0].style.zoom = 1; }, 50);
    }
};
eventHandlers['.image'] = {};
eventHandlers['.image']['load'] = function(event)
{
    $("#"+$(this).attr("id")+"_l").hide();
	$(this).show();
};
eventHandlers['.image']['error'] = function(event)
{
    $("#"+$(this).attr("id")+"_l").hide();
	$(this).show();
};
eventHandlers['.image']['click'] = function()
{
	if ($(this).attr("action_name") !== 'undefined')
	{
		if ($('[block_action="'+$(this).attr("action_name")+'"]').length > 0)
		{
			return;
		}
	}
	$("body").addClass("wait");
	$(this).attr("disabled",true);
    var ajaxurl = 'skylin/Image.php',
    data =  {'imageClick': $(this).attr("id")};
	$.post(ajaxurl, data,responseAndEnable($(this).attr("id")));
};	

eventHandlers['.splitter_bar'] = {};
eventHandlers['.splitter_bar']['mousedown'] = function(event)
{
	var id = $(this).attr("id").substr(0,$(this).attr("id").length-4);
	$("#"+id).toggleClass('splitter_closed');
	$("#"+id+"_left").toggleClass('splitter_left_closed');
    $("#"+id+"_right").toggleClass('splitter_right_closed');
	$("#"+id+"_bar").toggleClass('splitter_bar_closed');
	$("#"+id+"_button").toggleClass('splitter_button_closed');
	
	resizeTables($("#"+id));

	var ajaxurl = 'skylin/Splitter.php';
	data =  {'toggle': $(this).attr("id")};
	$.post(ajaxurl, data,response);
	
};

eventHandlers['.splitter_bar_reverse'] = {};
eventHandlers['.splitter_bar_reverse']['mousedown'] = function(event)
{
	var id = $(this).attr("id").substr(0,$(this).attr("id").length-4);
	$("#"+id).toggleClass('splitter_closed_reverse');
    $("#"+id+"_left").toggleClass('splitter_left_closed_reverse');
	$("#"+id+"_right").toggleClass('splitter_right_closed_reverse');
	$("#"+id+"_bar").toggleClass('splitter_bar_closed_reverse');
	$("#"+id+"_button_reverse").toggleClass('splitter_button_closed_reverse');

	resizeTables($("#"+id));
	
	var ajaxurl = 'skylin/Splitter.php';
	data =  {'toggle': $(this).attr("id")};
	$.post(ajaxurl, data,response);
	
};

eventHandlers['.splitter_bar_reverse300'] = eventHandlers['.splitter_bar_reverse'];


eventHandlers['.tab_close'] = {};
eventHandlers['.tab_close']['click'] = function(event)
{
	// Same choice as TabStrip::removeTabBySeq: the tab to the right, or to the left when closing the last one.
	// The server response removes the tab and checks the same radio.
	var tab = $(this).closest('li');
	if (tab.children('input.tab').prop('checked'))
	{
		var next = tab.nextAll('li').first();
		if (next.length == 0)
		{
			next = tab.prevAll('li').first();
		}
		if (next.length > 0)
		{
			next.children('input.tab').prop('checked', true);
			resizeTables(next);
		}
	}
	tab.hide();
	var ajaxurl = 'skylin/TabStrip.php';
	var data = {'close': $(this).attr("id")};
	$.post(ajaxurl, data, response);
};

eventHandlers['.search_lov_table-row'] = {};
eventHandlers['.search_lov_table-row']['mousedown'] = function(event)
{
	var ajaxurl = 'skylin/SearchLov.php';
	data =  {'selectLovRow': $(this).attr("id")};
	$.post(ajaxurl, data,response);
	
};	

eventHandlers['.searchLovFilter'] = {};
eventHandlers['.searchLovFilter']['keyup'] = function(event)
{
	var lovTable = $(this).parent().parent().parent().parent().find('.search_lov_container');
	//alert(lovTable.attr('cols') + '   ' + $(this).attr('filter'));
	var cols = lovTable.attr('cols').split(',');
	var colValues = new Array(cols.length);
	var filters = $(this).parent().parent().parent().find('.searchLovFilter');
	filters.each(function(index,value)
	{
		//alert( index + ": " + value.getAttribute("filter") + ": " +value.value);
		var filterName = value.getAttribute("filter");
		var filterValue = value.value;
		inner:
		for (var i = 0; i < cols.length;i++)
		{
			if (cols[i] == filterName)
			{
				if ((filterValue+'').length > 0)
				{
					colValues[i] = filterValue;
				}
				break inner;
			}
		}
	});
	
	var rows = lovTable.find('*');
	rows.each(function(rowIndex,rowValue)
	{
		var vis = true;
		$(rowValue).find('.search_lov_col').each(function(colIndex,colValue)
		{
			//alert(rowIndex+":"+colIndex+":"+$(colValue).html());
			if (colValues[colIndex] !== undefined)
			{
				var a = $(colValue).html().toUpperCase();
				var b = colValues[colIndex].toUpperCase();
				if ((a.indexOf(b) <= -1 && b.indexOf(a) <= -1) || (a.length == 0 && b.length > 0))
				{
					vis = false;
				}
			}
		});
		if (vis)
		{
			$(rowValue).show();
		}
		else
		{
			$(rowValue).hide();
		}
	});
};	

eventHandlers['.search_lov_close'] = {};
eventHandlers['.search_lov_close']['click'] = function(event)
{
	$(this).parent().parent().parent().html("");
};

eventHandlers['.anchor_panel_close'] = {};
eventHandlers['.anchor_panel_close']['click'] = function(event)
{
	$(this).parent().html("");
};

eventHandlers['.choiceLov'] = {};
eventHandlers['.choiceLov']['change'] = function(event)
{
	$(this).find('.blank').remove();
	var ajaxurl = 'skylin/ChoiceLov.php';
	data =  {'selectChoiceLovRow': $(this).val()};
	$.post(ajaxurl, data,response);
};

eventHandlers['.check'] = {};
eventHandlers['.check']['click'] = function(event)
{
	var ajaxurl = 'skylin/Checkbox.php';
	data =  {'checkBoxClick': $(this).attr("id")};
	$.post(ajaxurl, data,response);
};

eventHandlers['.file_upload'] = {};
eventHandlers['.file_upload']['change'] = function(event)
{
	//alert(event.target.files[0].name);
	var ajaxurl = 'skylin/File.php';
	var data = new FormData();
	data.append('FILE_UPLOAD_FILE', event.target.files[0]);
	data.append('FILE_UPLOAD',$(this).parent().attr("id"));
	$.ajax({url: ajaxurl,
		   data: data,
		   cache: false,
		   contentType: false,
		   processData: false,
		   type: 'POST',
		   success: response});
}
eventHandlers['.file_upload_trigger'] = {};
eventHandlers['.file_upload_trigger']['click'] = function(event)
{
	$(this).parent().children().first().trigger('click');
}

eventHandlers['.file_download'] = {};
eventHandlers['.file_download']['click'] = function(event)
{
	var parent = $(this).parent();
	var ajaxurl = 'skylin/File.php';
	data =  {'FILE_DOWNLOAD': $(this).parent().attr("id")};
	$.post(ajaxurl, data,function(data,status){response(data,status);download(parent.attr("urltodownload"));});

}

eventHandlers['.file_area'] = {};
eventHandlers['.file_area']['dragover'] = function(event)
{
	event.preventDefault();//without this the browser opens the dropped file instead of firing drop
	event.originalEvent.dataTransfer.dropEffect = this.getAttribute('uploadtoken') ? 'copy' : 'none';
}
eventHandlers['.file_area']['drop'] = function(event)
{
	event.preventDefault();
	var area = this;
	if (!area.getAttribute('uploadtoken'))
	{
		return;//not editable: the server gave this area no token
	}
	var entries = [];
	var items = event.originalEvent.dataTransfer.items;
	for (var i = 0; i < items.length; i++)
	{
		var entry = items[i].kind == 'file' ? items[i].webkitGetAsEntry() : null;
		if (entry)
		{
			entries.push(entry);
		}
	}
	Promise.all(entries.map(fileAreaFiles)).then(function(lists)
	{
		var files = lists.flat();
		if (files.length == 0)
		{
			return;
		}
		var labels = files.map(function(file)
		{
			if (!area.classList.contains('file_area_standalone'))
			{
				return null;
			}
			var label = document.createElement('div');
			label.textContent = file.name;
			area.appendChild(label);
			return label;
		});
		var worker = new Worker('skylin/file-upload-worker.js');
		worker.onmessage = function(msg)
		{
			if (msg.data.type == 'uploaded')
			{
				if (msg.data.ok && labels[msg.data.index] != null)
				{
					labels[msg.data.index].classList.add('file_area_uploaded_text');
				}
			}
			else if (msg.data.type == 'done')
			{
				worker.terminate();
				$.post('skylin/FileArea.php', {'FILE_AREA_UPLOAD': area.id}, response);
			}
		};
		worker.postMessage({url: new URL('restless.php?com.distantmelody.framework.skylin.services.builtin.FileUpload', document.baseURI).href,
							header: area.getAttribute('viewid')+'_'+area.getAttribute('uploadtoken')+'_',
							files: files});
	});
}

//the files of a dropped entry: the file itself, or every file under a directory
function fileAreaFiles(entry)
{
	return new Promise(function(resolve)
	{
		if (entry.isFile)
		{
			entry.file(function(file){resolve([file]);}, function(){resolve([]);});
			return;
		}
		if (!entry.isDirectory)
		{
			resolve([]);
			return;
		}
		var reader = entry.createReader();
		var children = [];
		function readMore()//readEntries answers in batches, and an empty batch at the end
		{
			reader.readEntries(function(batch)
			{
				if (batch.length == 0)
				{
					Promise.all(children.map(fileAreaFiles)).then(function(lists){resolve(lists.flat());});
					return;
				}
				children.push.apply(children, batch);
				readMore();
			}, function(){resolve([]);});
		}
		readMore();
	});
}


function templateEvent(name,id,data = null)
{
    var ajaxurl = 'skylin/Template.php',
    data =  {'templateEvent': id,
    		 'eventName':name,
    		 "eventData":data==null?null:JSON.stringify(data)};
    $.post(ajaxurl, data,response);
}



