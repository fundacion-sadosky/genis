define(['lodash'], function(_) {
    'use strict';

    var PedigreesDataCtrl = function($scope, $filter, pedigreeService, $routeParams, $modal, statsService, alertService, $timeout,$route,$location) {
        $scope.isProcessing = false;
        $scope.pageSize = 30;
        $scope.totalItems = 0;
        $scope.courtcaseId = $routeParams.id;

        localStorage.removeItem("nuevo");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.datepickers = {
            hourFrom : false,
            hourUntil : false
        };

        $scope.dateOptions = {
            initDate : new Date()
        };

        $scope.toggleDatePicker = function($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };


        $scope.status = [{label:""}].concat(["UnderConstruction","Active","Validated","Closed"]
            .map(function(s) { return {label: $filter('i18next')('pedigree.status.' + s), value: s}; }));

        console.log($route);
        console.log($location);

        $scope.options = {};

        $scope.pedigreeSearch = {};
        $scope.changePage = function() {
            $scope.initPedigrees();
        };
        $scope.initPedigrees = function() {
            $scope.pedigreeSearch.idCourtCase = parseInt($scope.courtcaseId);
            $scope.pedigreeSearch.pageSize = $scope.pageSize;
            if($scope.searchText){
                $scope.pedigreeSearch.input = $scope.searchText;
            }else{
                $scope.pedigreeSearch.input = undefined;
            }

            if($scope.currentPageNumber){
                $scope.pedigreeSearch.page  = $scope.currentPageNumber -1;
            }else{
                $scope.pedigreeSearch.page = 0;
            }
            $scope.pedigreeSearch.status = $scope.searchStatus;

            if($scope.hourFrom){
                $scope.pedigreeSearch.dateFrom = $scope.hourFrom;
                $scope.pedigreeSearch.dateFrom.setHours(0);
                $scope.pedigreeSearch.dateFrom.setMinutes(0);
                $scope.pedigreeSearch.dateFrom.setSeconds(0);

            }else{
                $scope.pedigreeSearch.dateFrom = undefined;
            }

            if($scope.hourUntil){
                $scope.pedigreeSearch.dateUntil = $scope.hourUntil;
                $scope.pedigreeSearch.dateUntil.setHours(0);
                $scope.pedigreeSearch.dateUntil.setMinutes(0);
                $scope.pedigreeSearch.dateUntil.setSeconds(0);

            }else{
                $scope.pedigreeSearch.dateUntil = undefined;
            }
            /*pedigreeService.getTotalCourtCasePedigrees($scope.pedigreeSearch).then(function (response) {
                $scope.totalItems = response.data;
            });*/
            if(localStorage.length > 0 && localStorage.getItem("searchPedigree")){
                $scope.pedigreeSearch = JSON.parse(localStorage.getItem("searchPedigree"));
                $scope.hourUntil = $scope.pedigreeSearch.dateUntil;
                $scope.hourFrom = $scope.pedigreeSearch.dateFrom;
                $scope.pageSize = $scope.pedigreeSearch.pageSize;
                $scope.currentPageNumber = $scope.pedigreeSearch.page;
                $scope.searchStatus = $scope.pedigreeSearch.status;
                $scope.searchText = $scope.pedigreeSearch.input;
                $scope.courtcaseId = $scope.pedigreeSearch.idCourtCase;
            }
            pedigreeService.getCourtCasePedigrees($scope.pedigreeSearch).then(function (response) {
                $scope.isProcessing = false;
                $scope.pedigrees = response.data;
                localStorage.removeItem("searchPedigree");

            });
        };
        $scope.init = function () {
            $scope.currentPageNumber = 1;

            $scope.initPedigrees();

        };

        $scope.init();

        $scope.search = function() {
            $scope.isProcessing = true;

            $scope.initPedigrees();
        };

        $scope.cleanFilters = function() {
            $scope.searchText = "";
            $scope.searchStatus = undefined;
            $scope.hourFrom = undefined;
            $scope.hourUntil = undefined;
            localStorage.removeItem("searchPedigree");
            $scope.search();
        };


        $scope.goToPedigree = function(courtCaseId, p) {
            localStorage.setItem("searchPedigree",JSON.stringify($scope.pedigreeSearch));
            if (p) {
                $location.url('/pedigree/' + courtCaseId + '/' + p.id);
            } else {
                localStorage.setItem("nuevo","pedigri");
                $location.url('/pedigree/'+ courtCaseId + '/0');
            }
        };
        $scope.goToConsistency = function(courtCaseId, p) {
            localStorage.setItem("searchPedigree",JSON.stringify($scope.pedigreeSearch));
            if (p) {
                $location.url('/pedigree-consistency/' + courtCaseId + '/' + p.id);
            }
        };
        $scope.goToTrace = function(courtCaseId, p) {
            localStorage.setItem("searchPedigree",JSON.stringify($scope.pedigreeSearch));
            if (p) {
                $location.url('/trace-pedigree/' + courtCaseId + '/' + p.id);
            }
        };
        $scope.maxDate = $scope.maxDate ? null : new Date();
        $scope.minDatePed = null;

        $scope.checkMaxMin = function(fieldName, fechaMin){
            var aux = $scope[fieldName];
            var min = $scope[fechaMin];
            var max = new Date();

            if(min === undefined || min === null ) {
                min = $scope.hourFrom;
            }
            if(max-aux < 0 ){
                alertService.info({message: $.i18n.t('alerts.date.before')});
                $scope[fieldName] = undefined;
            }else{
                if( min-aux > 0 ){
                    alertService.info({message: $.i18n.t('alerts.date.after')});
                    $scope[fieldName] = undefined;
                }
            }

        };

        $scope.checkMax = function(fieldName) {
            var aux = $scope[fieldName];
            var today = new Date();

            if(today-aux < 0 ){
                alertService.info({message: $.i18n.t('alerts.date.before')});
                $scope[fieldName] = undefined;
                $scope.minDatePed = null;
            }else{
                if(fieldName === 'hourFrom' ){
                    if(aux !== undefined || aux !== null) {$scope.minDatePed = aux; }
                    else { $scope.minDatePed = null; }
                }
            }

        };
        $scope.isDeleted = function(p) {
            return p.status === 'Deleted';
        };

        $scope.isClosed = function(p) {
            return (p.status === 'Closed' || p.status === 'Validated');
        };
        $scope.isActive = function(p) {
            return (p.status === 'Active');
        };

        $scope.deletePedigree = function(confirmed,profile) {
            var p = JSON.parse(profile);
            if(!confirmed || $scope.isDeleted(p) || $scope.isClosed(p) || $scope.isActive(p) ){
                return;
            }
            pedigreeService.canDelete(p.id).then(function(response) {
                // returns if the pedigree can be fisical deleted
                var editable = response.data;

                if (!editable) {
                    alertService.error({message: $.i18n.t('alerts.pedigree.deleteActivatedError')});
                } else {
                    pedigreeService.fisicalDelete(p.id, {}).then(function() {
                        alertService.success({message: $.i18n.t('alerts.genericDelete.success')});
                        $scope.search();
                    }, function(response) {
                        alertService.error({message: response.data});
                    });
                }
            });
        };
        $scope.isCourtCaseClosed = function() {
            return !_.isUndefined($scope.courtcase) && $scope.courtcase.status === 'Closed';
        };
    };

    return PedigreesDataCtrl;

});