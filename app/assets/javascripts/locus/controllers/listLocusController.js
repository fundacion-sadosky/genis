define(['lodash'], function(_) {

    'use strict';

    function ListLocusController($scope, locusService, alertService,$modal) {

        $scope.activeOption = -1;
        $scope.loadLocus();

        $scope.$watch('locusByAnalysisType', function() {
            if ($scope.locusByAnalysisType) {
                $scope.options = Object.keys($scope.locusByAnalysisType);
                $scope.show($scope.activeOption);
            }
        });
        // $scope.changeRequired = function(locus) {
        //     locusService.update(locus).then(function() {
        //         alertService.success({message: 'Se ha actualizado el marcador satisfactoriamente'});
        //         $scope.loadLocus();
        //         $scope.isProcessing = false;
        //     }, function(response) {
        //         alertService.error(response.data);
        //         $scope.isProcessing = false;
        //     });
        // };
        $scope.show = function(index) {
            $scope.activeOption = index;
            if (index === -1) {
                $scope.locusSearch = $scope.locus;
            } else {
                $scope.locusSearch = $scope.locusByAnalysisType[$scope.options[index]];
            }
            
            if ($scope.locusSearch.length === 0) {
                $scope.noResult = true;
            } else {
                $scope.noResult = false;
            }
        };
        
        $scope.remove = function(id) {
            $scope.isProcessing = true;
            locusService.delete(id).then(function() {
                alertService.success({message: $.i18n.t('alerts.locus.deleted')});
                $scope.loadLocus();
                $scope.isProcessing = false;
            }, function(response) {
                alertService.error(response.data);
                $scope.isProcessing = false;
            });
        };
        
        $scope.printLinks = function(links) {
            return links.map(function(l) { return l.locus; }).join(", ");
        };

        $scope.updateLocusRanges = function(locus,alias){
            console.log('updateLocusRanges locus:');
            console.log(locus);
            $scope.selectedLocus = locus;
            $scope.selectedLocusCopy = _.cloneDeep(locus) ;

            if(_.isUndefined(alias)){
                $scope.selectedAliasCopy = [];
            }else{
                $scope.selectedAliasCopy = _.cloneDeep(alias) ;
            }
            $scope.modalInstance = $modal.open({
                templateUrl:'/assets/javascripts/locus/views/update-locus.html',
                scope: $scope,
                keyboard: false
            });
        };

    }

    return ListLocusController;

});