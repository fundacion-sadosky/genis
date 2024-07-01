define(['lodash'], function() {
    'use strict';

    var GroupedProfilesModal = function($scope ,pedigreeService, $modal,alertService ,data) {
        $scope.idCourtCase = data.idCourtCase;
        $scope.globalCode = data.profile;
        $scope.totalItems = 0;
        $scope.pageNumber = 1;
        $scope.pageSize = 10;
        $scope.profilesSearch = {};
        $scope.activePedigrees = true;


        $scope.changePage = function() {
            $scope.searchProfiles();
        };



        $scope.searchProfiles = function () {
            $scope.isProcessing = true;
            $scope.profilesSearch.pageSize = $scope.pageSize;

            if($scope.profileGrouped){
                $scope.profilesSearch.searchText = $scope.profileGrouped;
            }else{
                $scope.profilesSearch.searchText = undefined;
            }

            if($scope.pageNumber){
                $scope.profilesSearch.page  = $scope.pageNumber -1;
            }else{
                $scope.profilesSearch.page = 0;
            }
            $scope.profilesSearch.idCourtCase = $scope.idCourtCase;
            $scope.profilesSearch.statusProfile = "Collapsed";
            $scope.profilesSearch.groupedBy = $scope.globalCode;

            pedigreeService.getTotalProfilesInactive($scope.profilesSearch).then(function (response) {
                $scope.totalItems = response.data;
            });
            pedigreeService.getProfilesInactive($scope.profilesSearch).then(function (response) {
                $scope.isProcessing = false;
                $scope.profiles = response.data;
            });

        };



        $scope.selectProfile= function(pd){
            $scope.isProcessing = true;
            var requestObj = {};
            var listRequest = [];
            requestObj.globalCode = pd.globalCode;
            requestObj.groupedBy =  $scope.globalCode;
            requestObj.courtcaseId = parseInt($scope.idCourtCase);
            listRequest.push(requestObj);

            $scope.dissasociate(listRequest);

        };

        $scope.dissasociateGrouped= function () {
            var listRequest = [];
            $scope.profiles.forEach(function (elem) {
                var requestObj = {};
                requestObj.globalCode = elem.globalCode;
                requestObj.groupedBy =  $scope.globalCode;
                requestObj.courtcaseId = parseInt($scope.idCourtCase);
                listRequest.push(requestObj);
            });

            if(listRequest.length>0){
                $scope.dissasociate(listRequest);
            }
        };

        $scope.dissasociate = function (listRequest) {
            $scope.isProcessing = true;
            pedigreeService.disassociateGroupedProfiles(listRequest).then(function (response) {
                $scope.pro = response.data;
                alertService.success({message: $.i18n.t('alerts.profile.ungroupSuccess')});
                $scope.isProcessing = false;
                $scope.searchProfiles();
            },function(response){
                $scope.pro = response.data;
                console.log('addProfiles response',response);
                $scope.isProcessing = false;
            });

        };

        $scope.utilizado = function(){

            pedigreeService.doesntHaveActivePedigrees($scope.idCourtCase).then(function (response) {
                $scope.activePedigrees = !response.data;
            }, function (response) {
                console.log(response);
            });

        };
        $scope.utilizado();
        $scope.searchProfiles();


        
    };
        return GroupedProfilesModal;

    });