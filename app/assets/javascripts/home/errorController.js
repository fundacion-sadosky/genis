define(['jquery'], function(jq) {
'use strict';

function ErrorController($scope, errorService, alertService) {

	$scope.closeError = function() {
		jq('#errorDiv').animate({height : "0%"}, 200);

	};

	errorService.onError(function(error) {
        alertService.error({'message': error.data});
		/*
		var errDoc = jq('#errorIframe')[0].contentDocument;
        errDoc.open();
        errDoc.write(error.data);
        errDoc.close();
        jq('#errorDiv').animate({height : "50%"}, 400);
        */
	});

}

return ErrorController;

});
