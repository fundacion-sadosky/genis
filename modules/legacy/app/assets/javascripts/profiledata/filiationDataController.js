/**
 * profiledata controllers.
 */
define(['jquery'], function() {
'use strict';
	
function FiliationDataController($scope, $log, profileDataService, $upload,cryptoService, alertService) {
	

	
	$scope.dateOptions = {
        initDate : new Date()
    };

    $scope.checkMax = function(fieldName) {
        var aux = $scope.$parent.profileData.dataFiliation[fieldName];
        var today = new Date();

        if(today-aux < 0 ){
            alertService.error({message: 'La fecha debe ser anterior a la actual.'});
            $scope.$parent.profileData.dataFiliation[fieldName] = undefined;

        }
    };

    $scope.maxDate = $scope.maxDate ? null : new Date();

    $scope.datepickers = {
        birthday : false
	};
	console.log($scope);

	if($scope.pictures.length === 0){
		$scope.pictures.push($scope.picturePlaceHolderImage);
	}
	
	if($scope.inprints.length === 0){
		$scope.inprints.push($scope.inprintPrintPlaceHolderImage);
	}
	
	if($scope.signatures.length === 0){
		$scope.signatures.push($scope.signaturePlaceHolderImage);
	}
	
	function sucessImagePost(type){
		return function(data){	
			switch(type) {
			case 'picture':
				$scope.pictures.push("/resources/temporary/" + data.substring(5));
					if($scope.pictures[0] === $scope.picturePlaceHolderImage) {
						$scope.pictures.shift();
					}
				break;
			case 'inprint':
				$scope.inprints.push("/resources/temporary/" + data.substring(5));
				if($scope.inprints[0] === $scope.inprintPrintPlaceHolderImage) {
					$scope.inprints.shift();
				}
				break;
			case 'signature':
				$scope.signatures.push("/resources/temporary/" + data.substring(5));
				if($scope.signatures[0] === $scope.signaturePlaceHolderImage) {
					$scope.signatures.shift();
				}
				break;
			}
		};
	}
	
	var errorImagePost = function() {
		alertService.error({message: 'error al mandar las imagenes'});
	};
	
	profileDataService.getFilesId().then(
		function(response){
			$scope.token.picture = response.data.filesId;
		}
	);
	
	profileDataService.getFilesId().then(
			function(response){
				$scope.token.inprint = response.data.filesId;
			}
	);
	
	profileDataService.getFilesId().then(
			function(response){
				$scope.token.signature = response.data.filesId;
			}
	);
		
	$scope.onFileSelect = function($files, type) {
		if($files && $files.length !== 0){	
			var ss = cryptoService.encryptBase64('/uploadImage');
			$log.log($files, $upload);
			for (var i = 0; i < $files.length; i++) {
				var file = $files[i];
				var uploadResults = $upload.upload({
					url: ss, 
					method: 'POST',
					fields: {filesId: $scope.token[type]},
					file: file
				});
				uploadResults
					.success( sucessImagePost(type) )//.success(type === 'picture' ? sucessPicturePost : (type === 'inprint' ? sucessInprintPost : sucessSignaturesPost))
					.error(errorImagePost);
			}
		}
	};
	
	$scope.toggleDatePicker = function($event, witch) {
		$event.preventDefault();
		$event.stopPropagation();
		$scope.datepickers[witch] = !$scope.datepickers[witch];
	};
		
}

return FiliationDataController;

});