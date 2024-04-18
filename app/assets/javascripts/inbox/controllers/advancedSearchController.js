define([], function() {
    'use strict';

    function AdvancedSearchController($scope,alertService) {
        $scope.filters = {};

        $scope.types = ['profileData', 'profileDataAssociation', 'matching', 'bulkImport', 'userNotification','pedigreeMatching','pedigreeLR','inferiorInstancePending','hitMatch','discardMatch','collapsing', 'profileUploaded'];

        $scope.datepickers = {
            hourFrom : false,
            hourUntil : false
        };

        $scope.dateOptions = {
            initDate : new Date()
        };


        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.toggleDatePicker = function($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };

        $scope.maxDate = $scope.maxDate ? null : new Date();
        $scope.minDate = null;

        $scope.checkMaxMin = function(fieldName, fechaMin){
            var aux = $scope.filters[fieldName];
            var min = $scope.filters[fechaMin];
            var max = new Date();

            if(min === undefined || min === null ) {
                min = $scope.hourFrom;
            }
            if(max-aux < 0 ){
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.filters[fieldName] = undefined;
            }else{
                if( min-aux > 0 ){
                    alertService.info({message: 'La fecha  debe ser posterior al campo desde.'});
                    $scope.filters[fieldName] = undefined;
                }
            }

        };

        $scope.checkMax = function(fieldName) {
            var aux = $scope.filters[fieldName];
            var today = new Date();

            if(today-aux < 0 ){
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.filters[fieldName] = undefined;
                $scope.minDate = null;
            }else{
                if(fieldName === 'hourFrom' ){
                    if(aux !== undefined || aux !== null) {$scope.minDate = aux; }
                    else { $scope.minDate = null; }
                }
            }

        };

        $scope.searchAdvanced = function() {
            $scope.resetSearch(-1);
            $scope.searchNotifications($scope.filters);
        };

        $scope.clean = function () {
            $scope.filters = {};
            $scope.minDate = null;
            $scope.show(0);
        };
        
    }
    
    

    return AdvancedSearchController;

});
