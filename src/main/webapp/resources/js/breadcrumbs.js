function initBreadcrumbs()
{
	alert('a');
	$("#breadcrumbs").mouseleave(function() { 
		bctimeout = setTimeout(function() { $("#breadcrumbs > a.bc").fadeOut(200); $("#breadcrumbs > a.info").fadeOut(200); }, 100);
	});
	$("#breadcrumbs").mouseenter(function() { clearTimeout(bctimeout); });
	$("#breadcrumbs > a.bclnk").mouseenter(function() {
		$("#breadcrumbs > a.bc").show(); $("#breadcrumbs > a.info").show();
	});
}