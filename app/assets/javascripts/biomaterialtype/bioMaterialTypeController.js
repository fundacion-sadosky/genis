define(['angular'], function(angular) {
	'use strict';

	function BioMaterialTypeCtrl ($scope, bioMaterialTypeService, $modal, alertService) {

        $scope.mode = '';

        $scope.selectBmt = function(b) {
            $scope.current = b;
        };

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

		function getBmts(){
			bioMaterialTypeService.getBioMaterialTypes().then(function(response){
				$scope.bmt = response.data;

				if ($scope.current) {
                    $scope.selectBmt($scope.bmt.filter(function(x){return x.id === $scope.current.id;})[0]);
					$scope.mode = 'edit';
				} else if (!$scope.current && $scope.bmt.length > 0) {
                    $scope.selectBmt($scope.bmt[0]);
					$scope.mode = 'edit';
				}
			});
		}

		$scope.addBmt = function(){
			$scope.selectBmt({});
			openBmtModal({}, 'add', onModalAddClose);
		};
		
		function onModalAddClose(bmt){
			$scope.selectBmt(bmt);
			$scope.save();
		}
		
		function onModalEditClose(bmt){
			$scope.mode = 'edit';
			if(angular.isObject(bmt)) { // update role
				$scope.selectBmt(bmt);
				$scope.update();
			} else { // delete role
				$scope.selectBmt(undefined);
				$scope.delete(bmt);
			}
		}
		
		function openBmtModal(bmt, mode, onModalClose) {
			$scope.mode = mode;
			
			$modal.open({
				templateUrl:'/assets/javascripts/biomaterialtype/views/bmtModal.html',
				controller : 'bmtModalController',
				resolve : {
					bmt: function() {
						return angular.copy(bmt);
					},
					mode: function() {
						return mode;
					}
				}
			}).result.then(function(bmt){
				onModalClose(bmt);
			});
		}
		
		$scope.addBmt = function(){
			$scope.selectBmt({});
			openBmtModal({}, 'add', onModalAddClose);
		};
		
		$scope.editBmt = function(bmt) {
			openBmtModal(bmt, 'edit', onModalEditClose);
		};
		
		$scope.updateBmt = function(bmt) {
			$scope.mode = 'edit';
			$scope.selectBmt(bmt);
		};
		
		$scope.delete = function(id) {
			bioMaterialTypeService.deleteBioMaterialType(id).then(
					function(){
						alertService.success({message: $.i18n.t('biomaterial.alert.deleteSuccess')});
						getBmts();
					}, 
					function(){
						alertService.error({message: $.i18n.t('biomaterial.alert.deleteFail')});
					});
			};
		
		$scope.update = function() {
			bioMaterialTypeService.updateBioMaterialType($scope.current).then(
					function(){
						alertService.success({message: $.i18n.t('biomaterial.alert.updateSuccess') });
						getBmts();
					}, 
					function(response){
						alertService.error({message: $.i18n.t('biomaterial.alert.updateFail') + response.data});
					});
			};
		
		$scope.save = function() {
			bioMaterialTypeService.addBioMaterialType($scope.current).then(
				function(){
					alertService.success({message: $.i18n.t('biomaterial.alert.addSuccess')});
					getBmts();
				}, 
				function(response){
					$scope.status = $.i18n.t('biomaterial.alert.addFail') + response.data.error;
				});
		};
		
		getBmts();
	}
	
	return BioMaterialTypeCtrl;

});