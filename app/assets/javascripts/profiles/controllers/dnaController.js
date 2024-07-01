define(['angular','lodash'], function(angular,_) {
    'use strict';

    function DnaController(
        $scope , $rootScope, $routeParams, $log, profileService,
        $route, $location, $modal, alertService, resourcesHelper, appConf) {
        console.log(resourcesHelper);
        $scope.loci = [];
        $scope.addedLocus = [];
        $scope.addedLocusIds = [];

        $scope.status = {current:'fill'};

        $scope.addedLocusById = {};
        $scope.newAnalysis = {};
        $scope.chkAnalysis = {};
        $scope.lab = "-"+appConf.labCode+"-";
        var filesId = "";
        $scope.saveLabels = function() {

            var arg = {
                'globalCode': $scope.profile._id,
                'labeledGenotypification' : $scope.profile.labeledGenotypification
            };

            profileService.saveLabels(arg).then(
                function(response) {
                    if (response.data) {
                        $log.log("profileService.saveLabels returns: " + response.data);
                        if(response.data.error){
                            alertService.error({message: response.data.error});
                        }else{
                            alertService.success({message: $.i18n.t('alerts.genericSuccess.executed')});

                            if ($routeParams.profileId){
                                $rootScope.f = true;
                                $rootScope.m = (response.data.matcheable === undefined) ? true: response.data.matcheable;
                                $route.reload();
                            }
                            else{
                                $location.path("/profile/" + $scope.newAnalysis.globalCode);
                            }
                        }
                    }

                    $scope.disableLabels();
                },
                function() {
                    alertService.error({message: $.i18n.t('alerts.error.common2')});
                });
        };

        $scope.showLabelsModal = function() {

            if ($scope.labelsAreUsed()) {
                var c = confirm("$.i18n.t('alerts.profileViews.tagInUse')");
                if (!c) { return; }
            }

            var labelsModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/profiles/views/labels-modal.html',
                controller: 'labelsModalController',
                resolve: {
                    labelsSets: function () {
                        return $scope.labelSets;
                    }
                }
            });

            labelsModalInstance.result.then(function (labelsList) {
                $scope.profile.labeledGenotypification = {};
                $scope.labels = labelsList;
            });

        };

        $scope.associateProfiles = function() {
            var profilesModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/profiles/views/associate-profiles-modal.html',
                controller: 'associateProfilesModalController',
                resolve: {
                    data: function () {
                        return {genotypification: $scope.profile.genotypification, subcategory: $scope.subcategory};
                    }
                }
            });

            profilesModalInstance.result.then(function (labelsGen) {
                var l = {};
                l[labelsGen.profile] = labelsGen.genotypification;
                $scope.profile.labeledGenotypification = l;
                $scope.labels = profileService.convertProfileToLabels(l);
            });
        };


        $scope.setLabels = function(label) {

            Object.keys($scope.labels).forEach(function (l){
                $scope.profile.labeledGenotypification[l] = angular.extend({}, $scope.profile.labeledGenotypification[l]);
            });
            if(angular.isDefined($scope.selectedAlleles)){
                $scope.selectedAlleles.forEach(function(element) {
                    var allele = element.alleleValue;
                    var locusId = element.locusId;

                    $scope.profile.labeledGenotypification[label][locusId] =
                        angular.extend([], $scope.profile.labeledGenotypification[label][locusId]);

                    $scope.profile.labeledGenotypification[label][locusId] = $scope.profile.labeledGenotypification[label][locusId].concat(allele);
                });
            }
            $scope.selectedAlleles = [];
        };

        $scope.addLocus = function(newLocus) {

            if (!$scope.newAnalysis || !$scope.newAnalysis.genotypification) {
                $scope.newAnalysis = {
                    globalCode : $scope.profile._id,
                    token: filesId,
                    genotypification : {},
                    type: $scope.activeAnalysis
                };
            }
            if (!$scope.chkAnalysis || !$scope.chkAnalysis.genotypification) {
                $scope.chkAnalysis = {
                    index: 0,
                    indexCh: 0,
                    genotypification: {}
                };
            }

            if (newLocus) {
                $scope.disableLabels();
                var locusGroup = angular.copy($scope.locusById[$scope.locusById[newLocus].idGroup]);
                var locus = $scope.locusById[newLocus];
                $scope.initLocus($scope.newAnalysis, $scope.chkAnalysis,locusGroup);

                $scope.formatLocusDna($scope.newAnalysis, $scope.chkAnalysis, locus);

                $scope.loci.push(locusGroup);
                $scope.addedLocus.push(locusGroup);
                $scope.addedLocusById[locusGroup.id] = locusGroup;

            }
        };

        $scope.locusAlreadyAdded = function(item) {
            return $scope.addedLocus.filter(function(l) { return l.id === item.id || (angular.isDefined(l.idGroup) && angular.isDefined(item.idGroup) && l.idGroup === item.idGroup) ; }).length === 0;
        };

        $scope.removeLocus = function(index) {
            var locusId =  $scope.loci[index].id;
            if(locusId){
                delete $scope.newAnalysis.genotypification[locusId];
                delete $scope.chkAnalysis.genotypification[locusId];
            }
            $scope.loci.splice(index,1);
            $scope.addedLocus.splice(index,1);
        };
        $scope.addRegion = function() {
            if($scope.possibleRegions && $scope.possibleRegions.length>0){
                var locus = $scope.possibleRegions.pop();
                $scope.addedRegions.push(locus);
                $scope.addLocus(locus);
                $scope.addLocus(locus+"_RANGE");
                $scope.newLocus=undefined;
            }
        };
        $scope.removeLocusMt = function(locusId) {

            if(locusId){
                delete $scope.newAnalysis.genotypification[locusId];
                delete $scope.chkAnalysis.genotypification[locusId];
                delete $scope.newAnalysis.genotypification[locusId+'_RANGE'];
                delete $scope.chkAnalysis.genotypification[locusId+'_RANGE'];
            }
            $scope.loci = _.remove($scope.loci, function(x) {
                return x.id === locusId;
            });
            $scope.addedLocus = _.remove($scope.addedLocus, function(x) {
                return x.id === locusId;
            });
            delete $scope.addedLocusById[locusId];
            delete $scope.addedLocusById[locusId+'_RANGE'];

            if($scope.addedRegions.length>0) {
                $scope.addedRegions.splice($scope.addedRegions.indexOf(locusId), 1);
                $scope.possibleRegions.push(locusId);
            }
        };
        $scope.isSTR = function() {
            if ($scope.analysisTypes && $scope.activeAnalysis && $scope.activeAnalysis !== -1) {
                return $scope.analysisTypes[$scope.activeAnalysis].name === "Autosomal";
            } else {
                return false;
            }
        };

        $scope.getPossibleLocus = function() {
            if ($scope.associated && $scope.isSTR() && $scope.activeAnalysis) {
                var validLocus = Object.keys($scope.associated.genotypification[$scope.activeAnalysis]);
                return $scope.locusOptions.filter(function(l) { return validLocus.indexOf(l.id) !== -1; });
            } else {
                return $scope.locusOptions;
            }
        };


    }

    return DnaController;

});


