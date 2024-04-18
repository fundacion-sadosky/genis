define(['lodash'], function(_) {
    'use strict';

    function ProfileApprovalController($scope, profileApprovalService,alertService,$q,$modal,bulkuploadService,locusService,profileService,cryptoService) {
        var $modalInstance = {};
        var modalInstanceEpg = null;
        $scope.pageSize = 25;
        $scope.totalItems = 0;
        $scope.currentPage = 1;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.init = function() {
            console.log('bulkuploadService',bulkuploadService);
            console.log('locusService',locusService);

            profileApprovalService.getTotalPendingProfiles().then(function(response){
                $scope.totalItems = response.data;
            });

            locusService.listFull().then(function(response) {
                $scope.locus = response.data;
                $scope.locusById = _.keyBy(_.map($scope.locus, function (o) {  return o.locus;}), 'id');

                profileApprovalService.getPendingProfiles($scope.currentPage,$scope.pageSize).then(function(response) {
                    $scope.profiles = response.data;

                    $scope.profiles.forEach(function(element){
                        if(!_.isUndefined(element.genotypification["1"])){
                            var genotypification =  element.genotypification["1"];
                            if(element.genotypification["4"]){
                                element.genotypificationMT =  _.transform(element.genotypification["4"], function(result, value, key) {
                                    var elem = {};
                                    elem.locus = key;
                                    elem.alleles = value;
                                    result.push(elem);
                                }, []);
                            }

                            element.genotypification = _.transform(genotypification, function(result, value, key) {
                                var elem = {};
                                elem.locus = key;
                                elem.alleles = value;
                                result.push(elem);
                            }, []);
                            _.forEach(element.genotypification,_.partial(bulkuploadService.fillRange,$scope.locusById,locusService.isOutOfLadder));
                        }
                    });
                }, function() {
                    alertService.error({message: 'Error al consultar los perfiles'});
                });

            });
        };
        $scope.verErrores = function(errores){
            alertService.error({message: errores});
        };
        $scope.approveSelected = function(){
            var request = [];
            if($scope.profiles!==undefined){
                for (var i = 0; i < $scope.profiles.length; i++) {
                    if ($scope.profiles[i].selected){
                        var p = {};
                        p.globalCode = $scope.profiles[i].globalCode;
                        request.push(p);
                    }
                }
            }
            profileApprovalService.approveProfiles(request).then(function() {
                alertService.success({message: 'Se aprobaron los perfiles'});
                if($scope.profiles!==undefined){
                    for (var i = 0; i < $scope.profiles.length; i++) {
                        if ($scope.profiles[i].selected) {
                            $scope.profiles.splice(i,1);
                        }
                    }
                }
            }, function(response) {
                alertService.error({message: response.data.message});
            });
        };

        $scope.selectAll = function(){
            if($scope.profiles!==undefined){
                for (var i = 0; i < $scope.profiles.length; i++) {
                    $scope.profiles[i].selected = !$scope.profiles[i].selected;
                }
            }
        };

        $scope.import = function(profile) {
            var request = [];

            if($scope.profiles!==undefined){
                for (var i = 0; i < $scope.profiles.length; i++) {
                    if ($scope.profiles[i].globalCode === profile.globalCode) {
                        var p = {};
                        p.globalCode = $scope.profiles[i].globalCode;
                        request.push(p);
                    }
                }
                profileApprovalService.approveProfiles(request).then(function() {
                    alertService.success({message: 'Se aprobó el perfil'});
                    if($scope.profiles!==undefined){
                        for (var i = 0; i < $scope.profiles.length; i++) {
                            if ($scope.profiles[i].globalCode === profile.globalCode) {
                                $scope.profiles.splice(i,1);
                            }
                        }
                    }
                }, function(response) {
                    alertService.error({message: response.data.message});
                });
            }
        };

        $scope.doReject = function(profile,res) {
            console.log(res);
            var deferred = $q.defer();
            profileApprovalService.rejectPendingProfile(profile.globalCode,res).then(function() {
                alertService.success({message: 'Se rechazó el perfil'});
                deferred.resolve();
                $scope.deleteFromTable(profile);
            }, function() {
                alertService.error({message: 'Error al rechazar el perfil'});
                deferred.reject();
            });

        };

        $scope.reject = function(profile) {
            $scope.showMotiveTextArea = false;

            profileApprovalService.getMotives().then(function(response) {
                $scope.motives = response.data;
            }, function() {
                $scope.motives = [];
            });

            $modalInstance = $modal.open({
                templateUrl:'/assets/javascripts/bulkupload/modalRejectProtoProfile.html',
                scope: $scope
            });

            $modalInstance.result.then(function(res){
                $scope.doReject(profile,res);
            });

        };

        $scope.closeModal = function(motiveText,selectedMotive){
            if(motiveText === false){
                $modalInstance.dismiss('cancel');
            }else{
                for (var i = 0; i < $scope.motives.length; i++) {
                    if($scope.motives[i].id === parseInt(selectedMotive) && !$scope.motives[i].freeText){
                        motiveText = $scope.motives[i].description;
                    }
                }
                $modalInstance.close({motive: motiveText,idMotive:selectedMotive});
            }
        };
        $scope.onChangeMotive = function(selectedMotive){

            $scope.showMotiveTextArea = false;
            for (var i = 0; i < $scope.motives.length; i++) {
                if($scope.motives[i].id === parseInt(selectedMotive) && $scope.motives[i].freeText){
                    $scope.showMotiveTextArea = true;
                    $scope.motiveText = "";
                }
            }

        };

        $scope.deleteFromTable = function(profile) {
            for (var i = 0; i < $scope.profiles.length; i++) {
                if ($scope.profiles[i].globalCode === profile.globalCode) {
                    $scope.profiles.splice(i, 1);
                }
            }
        };
        $scope.changePage = function() {
            $scope.init();
        };
        $scope.closeModalEpg = function () {
            modalInstanceEpg.close();
            $scope.profileIdEpg = null;
        };
        $scope.showElectropherograms = function(profileId){
            profileService.getElectropherogramsByCode(profileId).then(
                function(response) {
                $scope.profileIdEpg = profileId;
                $scope.epg = encryptedEpgs(profileId, response.data);

                modalInstanceEpg = $modal.open({
                    templateUrl:'/assets/javascripts/matches/views/electropherograms-modal-full.html',
                    scope: $scope
                });
            });
        };
        $scope.showFiles = function(profileId){
            profileService.getFilesByCode(profileId).then(
                function(response) {
                    $scope.profileIdEpg = profileId;
                    $scope.files = encryptedFiles(profileId, response.data);

                    modalInstanceEpg = $modal.open({
                        templateUrl:'/assets/javascripts/matches/views/files-modal-full.html',
                        scope: $scope
                    });
                });
        };
        function encryptedEpgs(profile, epgs) {
            return epgs.map(function(f){ return {path:cryptoService.encryptBase64('/profiles/' + profile + '/epg/' + f.fileId),name:f.name, fileId: f.fileId};});
        }
        function encryptedFiles(profile, files) {
            return files.map(function(f){ return {path:cryptoService.encryptBase64('/profiles/' + profile + '/file/' + f.fileId),name:f.name, fileId: f.fileId};});
        }
        $scope.init();

    }

    return ProfileApprovalController;

});