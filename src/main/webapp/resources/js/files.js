function upload (formId, uploadStatus, uniqueId, uploadingMsg, successMsg, failureMsg, successCallback)
{	
	var iframeId = "uploadiframe_" + uniqueId;
	
	// create iframe
	var iframe = $('<iframe name="' + iframeId + '" id="' + iframeId + '" style="display: none" />');
	$("body").append(iframe);

	// submit upload form to the iframe
	$("#" + formId).attr("target", iframeId);
	$("#" + formId).submit();

	$("#" + uploadStatus).html(uploadingMsg);
	$("#" + uploadStatus).removeClass("uploaded");
	$("#" + uploadStatus).addClass("uploading");

	// wait for the iframe to load
	$("#" + iframeId).load(function () {
        iframeContents = $("#" + iframeId)[0].contentWindow.document.body.innerHTML;

        var data = JSON.parse(iframeContents);

		if (data.status == "success")
		{
            $("#" + uploadStatus).html(successMsg);
            $("#" + uploadStatus).removeClass("uploading");
			$("#" + uploadStatus).addClass("uploaded"); 
			
			if (successCallback != null)
			{
				successCallback(data.originalFileName);
			}
		}
		else if (data.status == "error")
		{
			$("#" + uploadStatus).html(failureMsg);
		}
		else
		{
			$("#" + uploadStatus).html(failureMsg);
		}	
    });
}
