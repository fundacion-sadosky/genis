define([], function() {
'use strict';

function TotpPromptController($scope, totpPromptService) {

	$scope.showTotpDialog = false;
	
	totpPromptService.onTotpPrompt(function() {
		$scope.totp = undefined;
		$scope.showTotpDialog = true;
	});

	$scope.sendTotp = function() {
        $scope.showTotpDialog = false;
		totpPromptService.resolveTotpPromise($scope.totp);
	};

	$scope.cancelTotp = function() {
        $scope.showTotpDialog = false;
		totpPromptService.cancelTotpPromise();
	};
}

return TotpPromptController;

});
