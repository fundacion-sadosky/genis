define(['lodash'], function(_) {
'use strict';

var ProfileCourtcaseCtrl = function($scope, $filter, pedigreeService, $routeParams, $modal, statsService, alertService, $timeout,$route,$location,bulkuploadService,locusService) {
    $scope.isProcessing = false;
    $scope.pageSize = 30;
    $scope.totalItems = 0;
    $scope.totalCaseItems = 0;
    $scope.hideFilterLegend = true;
    $scope.loteLabel=_;
    $scope.statusProfile = {input: '', activo: true, agrupado: false};
    $scope.activo = true;


    $scope.initTotalCaseProfiles = function(){
        var searchTotalCaseProfiles = {};
        searchTotalCaseProfiles.idCourtCase = parseInt($scope.courtcaseId);
        searchTotalCaseProfiles.pageSize = $scope.pageSize;
        searchTotalCaseProfiles.page = 0;
        searchTotalCaseProfiles.statusProfile = null;


         pedigreeService.getTotalProfiles(searchTotalCaseProfiles,$scope.activeTab === 2).then(function (response) {
                $scope.totalCaseItems = response.data;
         });

    };

    $scope.initTotalCaseProfiles();
    $scope.courtcaseId = $routeParams.id;

    console.log($route);
    console.log($location);

    $scope.options = {};

    $scope.profileSearch = {};
    $scope.changePage = function() {
        $scope.initProfiles();
    };

    $scope.initProfiles = function() {

        if(!$scope.statusProfile.activo && !$scope.statusProfile.agrupado) {
            $scope.profiles = 0;
            $scope.totalItems = 0;
            $scope.isProcessing = false;
        }else{
        $scope.selectAllCheck = false;
        $scope.selectAllCheckToggle = false;
        $scope.profileSearch.idCourtCase = parseInt($scope.courtcaseId);
        $scope.profileSearch.pageSize = $scope.pageSize;
        if($scope.searchText){
            $scope.profileSearch.searchText = $scope.searchText;
        }else{
            $scope.profileSearch.searchText = undefined;
        }

        if($scope.currentPageNumber){
            $scope.profileSearch.page  = $scope.currentPageNumber -1;
        }else{
            $scope.profileSearch.page = 0;
        }

        if($scope.statusProfile.activo && $scope.statusProfile.agrupado){
            $scope.profileSearch.statusProfile = null;
        }else{


            if($scope.statusProfile.activo && !$scope.statusProfile.agrupado){
            $scope.profileSearch.statusProfile = "Active";
            }else{
                $scope.profileSearch.statusProfile = "Collapsed";
            }
        }

        pedigreeService.getTotalProfiles($scope.profileSearch , $scope.activeTab === 2).then(function (response) {
            $scope.totalItems = response.data;
        });
        pedigreeService.getProfiles($scope.profileSearch,$scope.activeTab === 2).then(function (response) {
            $scope.isProcessing = false;
            $scope.profiles = response.data;

            $scope.profiles.forEach(function(element){
                if(!_.isUndefined(element.genotypification["1"])){
                    var genotypification =  element.genotypification["1"];
                    element.genotypification = _.transform(genotypification, function(result, value, key) {
                        var elem = {};
                        elem.locus = key;
                        elem.alleles = value;
                        result.push(elem);
                    }, []);
                    _.forEach(element.genotypification,_.partial(bulkuploadService.fillRange,$scope.locusById,locusService.isOutOfLadder));
                }
            });

            $scope.hideFilterLegend = !$scope.searchText && ($scope.statusProfile.activo && $scope.statusProfile.agrupado) ;
        });
        }
    };
    $scope.init = function () {
        $scope.currentPageNumber = 1;

        locusService.listFull().then(function(response) {

            $scope.locus = response.data;

            $scope.locusById = _.keyBy(_.map($scope.locus, function (o) {
                return o.locus;
            }), 'id');

            $scope.initProfiles();

        });
    };

    $scope.init();

    $scope.disasociateSelected = function(confirmed){
        if(!confirmed){
            return;
        }
        var listRequest = [];

        $scope.profiles.forEach(function (element) {
            if(element.selected){
                var requestObj = {};
                requestObj.globalCode = element.globalCode;
                requestObj.courtcaseId = parseInt($scope.courtcaseId);
                listRequest.push(requestObj);
            }
        });
        if(listRequest.length>0){
            $scope.dissasociate(listRequest);
        }

    };
    $scope.selectAll = function(){
        $scope.selectAllCheckToggle = !$scope.selectAllCheckToggle;
        if($scope.profiles){
            $scope.profiles.forEach(function (element) {
                element.selected = $scope.selectAllCheckToggle;
            });
        }
    };
    pedigreeService.getCourtCaseFull($scope.courtcaseId).then(function(response){
        $scope.courtcase = response.data;
    });

    $scope.collapse = function() {
        var request = {};
        request.courtcaseId = parseInt($scope.courtcaseId);
        pedigreeService.collapse(request).then(function(){
            alertService.success({message: $.i18n.t('alerts.pedigree.searchStarted')});
        },function(){
            alertService.error({message: $.i18n.t('alerts.collapsing.collapsingError')});
        });
    };
    $scope.addBatchProfiles = function() {
        var batchesModalInstance = $modal.open({
            templateUrl:'/assets/javascripts/pedigree/views/search-batch.html',
            controller: 'searchBatchModalController',
            resolve: {
                data: function () {
                    return {
                        previousSearch: $scope.previousProfileSearch,
                        idCourtCase: parseInt($scope.courtcaseId),
                        caseType: $scope.courtcase.caseType
                    };
                }
            }
        });
        console.log(batchesModalInstance);
        batchesModalInstance.result.then(function (batches) {
            console.log('batches',batches);
            var batchesToImport = batches.map(function (element) {
                return element.idBatch;
            });
            var importBatchReq = {};
            importBatchReq.courtcaseId = parseInt($scope.courtcaseId);
            importBatchReq.batches = batchesToImport;
            if($scope.courtcase.caseType === "DVI"){ importBatchReq.tipo = 3; }
            else{ importBatchReq.tipo = 2;  }
            console.log('importBatchReq',importBatchReq);

            if(batchesToImport.length>0){
                pedigreeService.addBatches(importBatchReq).then(function(){
                    alertService.success({message: $.i18n.t('alerts.genericSuccess.operation')});
                    $scope.initTotalCaseProfiles();
                    $scope.initProfiles();
                },function(response){
                    alertService.error({message: response.data.message});
                });
            }

        });
    };
    $scope.addProfile = function() {
        var profilesModalInstance = $modal.open({
            templateUrl:'/assets/javascripts/pedigree/views/search-multiple-profile-modal.html',
            controller: 'searchMultipleProfileModalController',
            resolve: {
                data: function () {
                    return {
                        previousSearch: $scope.previousProfileSearch,
                        idCourtCase: parseInt($scope.courtcaseId),
                        tabActive: $scope.activeTab,
                        tipo : $scope.courtcase.caseType

                    };
                }
            }
        });
        profilesModalInstance.result.then(function (profiles) {
            var listRequest = [];
            profiles.forEach(function(p){
                var requestObj = {};
                requestObj.globalCode = p.globalCode;
                requestObj.courtcaseId = parseInt($scope.courtcaseId);
                listRequest.push(requestObj);
            });
            if(listRequest.length>0){
                var request = {};
                request.profiles = listRequest;
                request.isReference = $scope.activeTab === 2;

                pedigreeService.addProfiles(request).then(function(){
                    alertService.success({message: $.i18n.t('alerts.profile.associateSuccess')});
                    $scope.initTotalCaseProfiles();
                    $scope.initProfiles();
                },function(response){
                    console.log('addProfiles response',response);
                    alertService.error({message: response.data.message});
                });
            }

        });
    };
    $scope.dissasociate = function(listRequest) {
        pedigreeService.removeProfiles(listRequest).then(function(response){
            console.log('addProfiles response',response);
            alertService.success({message: $.i18n.t('alerts.profile.disassociateSuccess')});
            $scope.initTotalCaseProfiles();
            $scope.initProfiles();
        },function(response){
            console.log('addProfiles response',response);
            alertService.error({message: $.i18n.t('alerts.profile.disassociateError')});
        });
    };
    $scope.removeProfile = function(confirmed,globalCode) {
        if(!confirmed){
            return;
        }
        var requestObj = {};
        var listRequest = [];
        requestObj.globalCode = globalCode;
        requestObj.courtcaseId = parseInt($scope.courtcaseId);
        listRequest.push(requestObj);
        $scope.dissasociate(listRequest);
    };

    $scope.searchProfile = function() {
        $scope.isProcessing = true;

        $scope.initProfiles();
    };

    $scope.isCourtCaseClosed = function() {
        return !_.isUndefined($scope.courtcase) && $scope.courtcase.status === 'Closed';
    };

    $scope.tieneHijos = function (p) {
        if(p.groupedBy === undefined || p.groupedBy !== p.globalCode ){
            return true;
        }else{
            return false;
        }
    };


    function perfilesAsociados(p) {
     $modal.open({
         templateUrl: '/assets/javascripts/pedigree/views/grouped-profiles-modal.html',
         controller: 'groupedProfilesModal',
         resolve: {
             data: function () {
                 return {
                     idCourtCase: parseInt($scope.courtcaseId),
                     profile: p
                 };
             }
         }
     }).result.then(function () {
         $scope.initTotalCaseProfiles();
         $scope.init();

     });
    }


    $scope.perfilesAsociados = function (p) {
        perfilesAsociados(p);
    };

    $scope.agruparManual= function(){
        $location.url('/manual-collapsing'+'?courtCaseId='+$scope.courtcaseId);

    };



};

return ProfileCourtcaseCtrl;

});