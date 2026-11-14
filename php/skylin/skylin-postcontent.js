
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

var ws;
function getWsReadyState()
{
	return ws.readyState;
}
wsConnect();
