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
		/*
		    var ajaxurl = 'skylin/TextField.php',
	        data =  {'change': $(this).attr("id")+","+$(this).val()};
	        $.post(ajaxurl, data,response);
			var ajaxurl = 'skylin/TextField.php';
			data =  {'linkClick': $(this).attr("id")};
			$.post(ajaxurl, data,response);
			*/
			var ajaxurl = 'skylin/TextField.php',
	        data =  {'change': $(this).attr("id")+","+$(this).val(),
	        		 'linkClick': $(this).attr("id")};
	        $.post(ajaxurl, data,response);
			var ajaxurl = 'skylin/TextField.php';
			e.preventDefault();
		}
		if ($(this).attr("sk_type") == 'number')
		{
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


	eventHandlers['.table-row_toggle'] = {};
	eventHandlers['.table-row_toggle']['click'] = function(event)
	{
		var ajaxurl = 'skylin/Table.php';
		id = $(this).attr("id");
		id = id.substring(0,id.length-7);
		data =  {'toggleRow': id};
		$.post(ajaxurl, data,response);
	};
	
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
	
	eventHandlers['.splitter_bar_reverse300'] = eventHandlers['.splitter_bar_reverse'];

	
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
	
	//prevents firefox from trying to "open" dragged elemented when they are dropped.
	window.addEventListener("dragover",function(e){
		  e = e || event;
		  e.preventDefault();
		},false);
		window.addEventListener("drop",function(e){
		  e = e || event;
		  e.preventDefault();
		},false);
	
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
	
	
	wsConnect();
    		      	
});

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
		    port = 81;
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
    		            	if (event.data == "ping")
    		            	{
    		            		ws.send("PING,"+topAppId+","+getCookie("PHPSESSID"));
    		            	}
    		            	else
    		            	{
    		            		response(event.data,null);
    		            	}
    		            };
    		            ws.onclose = function (event) {
    		                setTimeout(wsConnect,5000);
    		                
    		            };
    		        	ws.onerror = function(error) {
    		      		};

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
	xhr.open("POST", "/restless.php?skylin.services.builtin.MessageAM", true);
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







