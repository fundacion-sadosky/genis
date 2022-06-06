define([], function() {
	'use strict';

	function StatsController($scope, $location, $log, populationBaseService, $upload, $modal, cryptoService, userService, alertService) {
		
		$scope.thetaRe = new RegExp("^0\\.?[0-9]{0,3}$|^1$");
		$scope.statsModels = populationBaseService.getStatModels();
		var modalInstance = null;
		var vector = [];

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
		
		var hw = {id: 'HardyWeinberg',name: 'Hardy-Weinberg', enableTheta: false, show: true};
		var nrcii4 = {id: 'NRCII41',name: 'NRC II Recommendation 4.1', enableTheta: true, show: true};
		var nrcii410 = {id: 'NRCII410',name: 'NRC II Recommendation 4.10', enableTheta: true, show: true};
		
		var refreshBases = function() {
			populationBaseService.getAllBasesNames().then(
					function(response){
						$scope.baseNames = response.data;
			});
		};
		
		$scope.giveStatsModels = function(theta){
			vector = [];
			if(theta !== undefined){
			if(theta === 0){
				$scope.popBaseFreq.model = hw.id;
				vector.push(hw);
			}else{
				vector.push(nrcii4);
                vector.push(nrcii410);
			}
			}
			return vector;
		};
		
		$scope.resolveName = function(id){
			var s = $scope.statsModels.filter(function(elem){
				return id === elem.id;
			});
			
			return s[0].name;
		};
		
		$scope.disable = function(permission,isDefault){
			var user = userService.getUser();
			var enable = user.permissions.indexOf(permission) > -1;
			if(enable) {
				return isDefault;
			} else {
				return true;
			}
		};
		
		$scope.closeModal = function(){
			modalInstance.close();
		};
		
		function onSuccessInsert(data){
			if($scope.baseNames.length === 0){
				$scope.setDefault($scope.popBaseFreq.name);
			}
			
			refreshBases();
			var name = $scope.popBaseFreq.name;
			$scope.popBaseFreq = "";
			$scope.fileName="";
			$scope.selectedFiles = undefined;
			$scope.nameForm.$setPristine();
			alertService.success({message: name+": se insertaron "+data.inserts+" registros"});
			$scope.csvFile = undefined;
		}
		
		var sucessFilePost = function(data) {
			
			if (data.status === 'Incomplete'){
				
				var fminModal = $modal.open({
					templateUrl:'/assets/javascripts/stats/views/fmin.html',
					controller: 'fminController',
					resolve: {
						data: function () {
							return {loci: data.loci};
						}
					}
				}); 
				
				fminModal.result.then(function (fmin) {
					populationBaseService.insertFmin(data.key, fmin).then(
						function(response){
							onSuccessInsert(response.data);
					},
						function(response){
						errorFilePost(response);
					});
					
				}, function () {
				});
				
			}else if (data.status === 'Invalid'){
				alertService.error({message: data.errors});
            }else {
				onSuccessInsert(data);
			}
		};

		var errorFilePost = function(response) {
			var errorArray;
			if(response.length){
				errorArray = [].concat(response);
			}else{
				errorArray = [response.message];
			}
			alertService.error({message: errorArray.join('. ')});
		};
		
		$scope.setDefault = function(name) {
			populationBaseService.setAsDefault(name).then(function(response){
				$log.log(response);
				
				if (response.data > 0) { alertService.success({message: name + ": fue seteada como default"}); }
				else { alertService.error({message: name + ": no pudo ser seteada como default"}); }

				refreshBases();
			});
		};
		
		$scope.goTo = function(dest) {
			$location.path('/stats/' + dest);
		};
		
		$scope.onCSVSelect = function($files){
			$scope.selectedFiles = $files;
			$scope.fileName = $files[0].name;
		};

		refreshBases();
		
		$scope.toggleStateBase = function(name,state) {
			$log.log('deleting base named: '+name);
			populationBaseService.toggleStateBase(name).then(function(response){
				$log.log('deleted' + response);
				var normState = (state) ? "activado" : "desactivado";
				$scope.responseAction = name+" :se cambio el estado a "+normState;
				refreshBases();
			});
		};
		
		var isUniqueName = function(name){
			for(var index in $scope.baseNames){
				if($scope.baseNames[index].name.toUpperCase() === name.toUpperCase() ){
					return false;
				}
			}
			return true;
		};
		
		$scope.saveFileCSV = function() {
			
			if(!$scope.popBaseFreq || !$scope.popBaseFreq.name) {
				alertService.error({message: 'Debe especificar el nombre de la BD'});
				return;
			}
			
			if($scope.popBaseFreq.theta === 0 && $scope.popBaseFreq.model !== 'HardyWeinberg'){
				alertService.error({message: 'Debe especificar el nombre de la BD'});
				return;
			}
			
			if(!isUniqueName($scope.popBaseFreq.name)){
				alertService.error({message: 'Ya fue ingresada una base el mismo nombre'});
				return;
			}
			
			var ss = cryptoService.encryptBase64('/populationBaseFreq');
			var file = $scope.csvFile;
			$scope.upload = $upload.upload({
				url: ss, 
				method: 'POST',
				fields: {baseName: $scope.popBaseFreq.name, baseTheta: $scope.popBaseFreq.theta, baseModel: $scope.popBaseFreq.model},
				file: file,
			}).success(sucessFilePost)
			.error(errorFilePost);				
		};
		
	}

	return StatsController;

});