$(document).ready(function()
{
	/*
	if (window.name.length > 0)
	{
		document.cookie = "PHPSESSID="+window.name;
		window.name = "";
	}
	*/
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
	    var ajaxurl = 'skylin/TextField.php',
        data =  {'change': $(this).attr("id")+","+$(this).val()};
	    $(this).parent().prop('class','TextField_tip_disabled');
        $.post(ajaxurl, data,response);
    };
    
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
		    var ajaxurl = 'skylin/TextField.php',
	        data =  {'change': $(this).attr("id")+","+$(this).val()};
	        $.post(ajaxurl, data,response);
			var ajaxurl = 'skylin/TextField.php';
			data =  {'linkClick': $(this).attr("id")};
			$.post(ajaxurl, data,response);
		}
		if ($(this).attr("sk_type") == 'number')
		{
			// Allow: backspace, delete, tab, escape, enter and .
			if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 110, 190]) !== -1 ||
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
	
	eventHandlers['.table-row-hover']['mousedown'] = function(event)
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
	
	eventHandlers['.tab_close'] = {};
	eventHandlers['.tab_close']['click'] = function(event)
	{
		//alert($(this).attr("id"));
		var ajaxurl = 'skylin/TabStrip.php';
		data =  {'close': $(this).attr("id")};
		$.post(ajaxurl, data,response);
		
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
	
	refreshListeners($(document));
	
	$(document).on('mousemove', function(e){
		
		var adjustX = e.pageX + hoverInfoWidth - $(window).width();
		
		
		
		if (adjustX < 0)
		{
			adjustX = 0;
		}

	    $('#hover_info').css({
	       left:  e.pageX - adjustX,
	       top:   e.pageY + 5
	    });
	});
	$('#hover_info').hide();
	
	$(window).on('resize', function()
	{		
		resizeTables($('body'));
	});
	
	
	
	var loc = window.location, new_uri;
	if (loc.protocol === "https:") {
	    new_uri = "wss:";
	} else {
	    new_uri = "ws:";
	}
	new_uri += "//" + loc.host + ":1001";
	new_uri += loc.pathname + "SessionSocket";
    		                //ws = new WebSocket('ws://localhost:8090/war_test/SessionSocket');
	ws = new WebSocket(new_uri);

    		            ws.onopen = function () {
    		                ws.send(getCookie("PHPSESSID"));
    		            };
    		            ws.onmessage = function (event) {
    		                //alert('Received: ' + event.data);
    		            	response(event.data,null);
    		            };
    		            ws.onclose = function (event) {
    		                //alert('Info: WebSocket connection closed, Code: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));
    		            };
    		        	ws.onerror = function(error) {
    		      		  //alert('WebSocket Error: ' + error);
    		      	};
    		      	
    		      	
    console.log(getCookie("PHPSESSID"));		
});

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
			if (coms.size() > 0)
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
	
	setTimeout(function(){ resizeTables(com); }, 10);
	
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

function resizeTables(com)
{
	$.each(com.find(".table_frozen").addBack(".table_frozen"), function( i, table ) 
	{		
		resizeTable(table);
	});
}
function resizeTable(table)
{
	var width = $(table).width();
	width = width - 2;//prevents extra horizontal scrollbar from forming in certian setups. Not sure why. 
	var totalwidth = $(table).attr("totalwidth");
	var leftwidth = $(table).attr("leftwidth");
	totalwidth = Math.min(width,totalwidth);
	var newRightwidth = Math.max($(table).attr("minrightwidth"),totalwidth-leftwidth);
	var navLength = parseInt(leftwidth) + parseInt(newRightwidth);
	newRightwidth = newRightwidth - $(table).attr("paddingRight");
	$(table).find(".table_frozen_row").css({"width":newRightwidth});
	$(table).find(".nav").css({"width":navLength});
}

function download(link)
{
	document.getElementById('download_iframe').src = link;
}

var test = false;
function processMessage(type,message)
{
	if (type == 'setdiv')
	{
		var seperator = message.indexOf("-");
		var id = "#"+message.substring(0,seperator);
		var value = message.substring(seperator+1,message.length);
		var newCom = $(value);
		$(id).replaceWith(newCom);
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
	
		var row = '<tr>\
		<td><img src="skylin/images/'+type+'.png" height="16px" width="16px"></td>\
		<td style="max-width:380px;word-wrap: break-word;">&nbsp'+value+'</td> \
	  </tr>';
		
		$("#sk_message_list").append(row);
	}
	else if (type == 'openwindow')
	{
		window.open(message);
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







