define([ 'angular', 'angular', 'jquery','lodash'], function(ng, angular, $,_) {
'use strict';

var OriginalCtrl = function($scope, pedigreeService, $log, $routeParams, $modal, $route, alertService, $timeout, hotkeys) {
	$scope.courtcaseId = $routeParams.courtcaseId;
	$scope.hasProfilesAssociated = false;
	if($scope.courtcaseId){
        pedigreeService.getCourtCaseFull($scope.courtcaseId).then(function(response){
            $scope.isDVI = response.data.caseType === "DVI";
        });
    }
    var searchTotalCaseProfiles = {};
    searchTotalCaseProfiles.idCourtCase = parseInt($scope.courtcaseId);
    searchTotalCaseProfiles.pageSize = 1;
    searchTotalCaseProfiles.page = 0;
    searchTotalCaseProfiles.statusProfile = null;
    pedigreeService.getTotalProfiles(searchTotalCaseProfiles,true).then(function (response) {
        if(response.data){
            $scope.hasProfilesAssociated = true;
            console.log("hasProfilesAssociated",$scope.hasProfilesAssociated);
        }else{
            pedigreeService.getTotalProfiles(searchTotalCaseProfiles,false).then(function (response) {
                if(response.data){
                    $scope.hasProfilesAssociated = true;
                }
                console.log("hasProfilesAssociated",$scope.hasProfilesAssociated);
            });
        }
    });
    $scope.pedigreeId = $routeParams.pedigreeId;
    $scope.selection = { allSelected: false };
    $scope.unknownIndex = 2;
    $scope.profile= {};

    $scope.previousProfileSearch = "";

    var network;

    $scope.guardo = true;

    $scope.drawGraph = function (nodes) {
        var promise = $scope.createNetwork(nodes, "genogram", true).then(function(n){
            network = n;
            network.on('select',function () {
                var selected = network.getSelectedNodes();
                nodes.forEach(function (n) {
                    n.selected = selected.indexOf(n.alias) > -1;
                });
                if (selected.length > 0 || selected.length !== 0 ) {
                    $scope.$parent.elegi(false);
                }else{
                    $scope.$parent.elegi(true);
                }
                $scope.$apply();
            });
            return network;
        });

        $scope.updateOptions($scope.pedigree);

        return promise;
    };
    
    $scope.changeAllSelection = function(value) {
      $scope.pedigree.genogram.forEach(function(n) { $scope.changeSelection(n,value); });
    };

    $scope.changeSelection = function(node, value) {
        node.selected = value;
        if (!node.selected && $scope.selection.allSelected) {
            $scope.selection.allSelected = false;
        }
        var selected = network.getSelectedNodes();
        if (value) {
            network.selectNodes(selected.concat(node.alias));
        } else {
            var index = selected.indexOf(node.alias);
            selected.splice(index,1);
            network.selectNodes(selected);
        }
        if(value){
            $scope.elegi(false);
        }else {
            toggleCopyButton();
        }
    };

	function rollback() {
		$scope.pedigree = angular.copy($scope._pedigree);
	}
	
	function commit() {
		$scope._pedigree = angular.copy($scope.pedigree);
	}
	
	function existsAlias(node) {
		return $scope._pedigree.genogram.some(function(n) { return n.alias === node.alias; });
	}
	
	var emptyNode = {idFather: undefined, idMother: undefined, globalCode: undefined, unknown: false, sex: 'Unknown', alias: '', isReference: undefined};
	$scope.newNode = ng.copy(emptyNode);
	
	$scope.addNode = function() {
       $scope.guardo = false;
        if (existsAlias($scope.newNode)) {return;}
		var node =  ng.copy($scope.newNode);
		$scope.newNode = ng.copy(emptyNode);
        $('#aliasId').focus();
        $scope.pedigree.genogram.push(node);
        $scope.drawGraph($scope.pedigree.genogram).then(function (network) {
            pintarSelecionados(network);
        });
		commit();
	};
	
	$scope.removeNode = function(node) {
        $scope.guardo = false;

        if ($scope.pedigree.genogram.some(
                function (i) {
                    return (ng.isDefined(i.idMother) && i.idMother === node.alias) || (ng.isDefined(i.idFather) && i.idFather === node.alias);
                })) {
            alertService.error({message: $.i18n.t('alerts.node.parentError')});
            return;
        }

        if (node.unknown) {
            alertService.error({message: $.i18n.t('alerts.node.unknownError')});
            return;
        }

        for (var i = 0; i < $scope.pedigree.genogram.length; i++) {
            if ($scope.pedigree.genogram[i].alias === node.alias) {
                break;
            }
        }

        $scope.pedigree.genogram.splice(i, 1);
        $scope.drawGraph($scope.pedigree.genogram).then(function(network){
            pintarSelecionados(network);
        });

        commit();
        toggleCopyButton();

	};

    $scope.addNewScenario = function(unknown, profile, matchId) {
      $scope.guardo = false;
        var nodes = angular.copy($scope.pedigree.genogram);
        var ukNode = nodes.filter(function(n) { return n.alias === unknown; })[0];
        ukNode.globalCode = profile;
        var scenario = {pedigreeId: $scope.pedigreeId, genogram: nodes, frequencyTable: $scope.pedigree.frequencyTable, matchId: matchId,mutationModelId: $scope.pedigree.mutationModelId};
        $scope.addScenario(scenario);
        $scope.updateOptions(scenario);
    };

	$scope.save = function() {
        $scope.pedigree.assignee = $scope.courtcase.assignee;
        $scope.pedigreeMetadata.assignee = $scope.courtcase.assignee;
        $scope.pedigreeMetadata.name = $scope.$parent.pedigreeName;
        $scope.pedigreeData = {};
        $scope.pedigreeData.pedigreeMetaData = $scope.pedigreeMetadata;
        $scope.pedigree.caseType = $scope.courtcase.caseType;
        $scope.pedigree.idCourtCase = $scope.courtcaseId;
        $scope.pedigreeData.pedigreeGenogram = $scope.pedigree;
        $scope.pedigreeMetadata.courtCaseName = $scope.courtcase.internalSampleCode;
        if(!_.isUndefined($scope.copiedFrom)){
            $scope.pedigreeData.copiedFrom = $scope.copiedFrom;
            $scope.copiedFrom = undefined;
        }
        pedigreeService.createCompletePedigree($scope.pedigreeData).then(function(response) {
            $scope.guardo = true;
            if (response.data !== Number($scope.pedigreeId)) {

                $scope.pedigreeId = response.data;
                $scope.pedigree._id = $scope.pedigreeId;
                $routeParams.pedigreeId = "" + $scope.pedigreeId;
                $route.updateParams({
                    pedigreeId: $routeParams.pedigreeId
                });
                $route.reload();
            }
            alertService.success({message: $.i18n.t('alerts.genericSuccess.operation')});

        }, function(response){
            alertService.error({message: $.i18n.t('error.message') + (response.data ? response.data : response)});
            return;
        });

    };
	
	$scope.rAlias = new RegExp(/^[a-zA-Z0-9\-]{1,15}$/);
	$scope.isNodeValid = function(node) {
		return node && $scope.rAlias.test(node.alias) && node.alias && node.sex;
	};
	
	$scope.changeNodeAlias = function(node){
        $scope.guardo = false;
		if (!existsAlias(node)) {
			$scope.changeNode(node);
        } else {
			rollback();
		}
	};
	
	$scope.changeNode = function(node){
      $scope.guardo = false;
		if ($scope.isNodeValid(node)) {
			var fathers =
				$scope.pedigree.genogram.filter(function(i){return $scope.isMale(i);}).map(function(i){return i.alias;});
				
			var mothers = 
				$scope.pedigree.genogram.filter(function(i){return $scope.isFemale(i);}).map(function(i){return i.alias;});

			$scope.pedigree.genogram.forEach(function(n){
				if (fathers.indexOf(n.idFather)===-1) {n.idFather = undefined;}
				if (mothers.indexOf(n.idMother)===-1) {n.idMother = undefined;}
			});
			
			commit();
			$scope.drawGraph($scope.pedigree.genogram);
			
		} else {
			rollback();
		}
	};
	
	$scope.cancel = function(){
		$route.reload();
	};
	
	$scope.setUnknownIndividual = function(node) {
        if (node.unknown) {
            var oldAlias = node.alias;
            $scope.changeUnknownAlias(node);
            $scope.pedigree.genogram.filter(function (x) {
                if (x.alias === oldAlias) {
                    x.unknown = node.unknown;
                    x.alias = node.alias;
                }
            });
        }

		$scope._pedigree.genogram = $scope.pedigree.genogram;
		$scope.drawGraph($scope.pedigree.genogram);
	};

    $scope.changeUnknownAlias = function(node){
        node.alias = 'PI' + $scope.getUnknownIndex();
    };

    $scope.getUnknownIndex = function(){
        var unknowns = $scope.pedigree.genogram.filter(function(individual){
            return individual.alias.includes('PI');
        });

        if ($scope.newNode.alias.includes('PI')){
            unknowns.push($scope.newNode);
        }

        var maxUnknownExistingIndex = 0;
        unknowns.forEach(function(unknown){
            var index = parseInt(unknown.alias.substring(2));
            if (!isNaN(index) && maxUnknownExistingIndex < index){
                maxUnknownExistingIndex = index;
            }
        });

        return maxUnknownExistingIndex + 1;
    };

    $scope.hasEveryUnknownParents = function(){
        var unknowns = $scope.pedigree.genogram.filter(function(individual){
            return individual.unknown;
        });

        var result = true;
        unknowns.forEach(function(unknown){
            if (!unknown.idFather || !unknown.idMother){
                result = false;
            }
        });

        return result;
    };

	$scope.addProfile = function(node) {
       $scope.guardo = false;
		var profilesModalInstance = $modal.open({
			templateUrl:'/assets/javascripts/pedigree/views/search-profile-modal.html',
			controller: 'searchProfileModalController',
            resolve: {
                data: function () {
                    return {
                        previousSearch: $scope.previousProfileSearch,
                        idCourtCase: parseInt($scope.courtcaseId),
                        profiles: $scope.pedigree.genogram
                    };
                }
            }
		});

		profilesModalInstance.result.then(function (p) {
            node.globalCode = p.globalCode;
            $scope.profile.idCourtCase = parseInt($scope.courtcaseId);
            $scope.profile.globalCode =node.globalCode;

            pedigreeService.profileNodo($scope.profile).then(function(response) {
                node.isReference = response.data;
                $scope.changeNode(node);
                $scope.previousProfileSearch = p.previousSearch;
            });
        });
	};

    $scope.disassociateProfile = function(node) {
       $scope.guardo = false;
        var disassociateModalInstance = $modal.open({
            templateUrl:'/assets/javascripts/pedigree/views/disassociate-profile.html',
            controller: 'disassociateController',
            resolve:
            {
                node: function() {
                    return node;
                }
            }
        });

        disassociateModalInstance.result.then(function (value) {
            if (value) {
                delete node.globalCode;
                $scope.changeNode(node);
            }
        });
    };

    var changeStatus = function(status) {
        $scope.pedigree.status = status;
        $scope.pedigree.processed = false;
        pedigreeService.changePedigreeStatus($scope.pedigree._id, status, $scope.pedigree).then(function() {
            alertService.success({message: $.i18n.t('alerts.genericSuccess.operation')});
            $scope.pedigree.status = status;
            $scope.pedigree.processed = true;
            $scope.$parent.$parent.$parent.pedigreeStatus= $scope.pedigree.status;
        }, function(response) {
            alertService.error({message: response.data});
        });
    };

    $scope.closePedigree = function() {

        pedigreeService.canEdit($scope.pedigreeId).then(function(response) {
            // returns if the pedigree can be close
            var editable = response.data;

            if (!editable) {
                alertService.error({message: $.i18n.t('alerts.pedigree.closeError')});
            } else  {
                changeStatus('Closed');
            }
        });
    };

    $scope.deactivate = function() {

        pedigreeService.canEdit($scope.pedigreeId).then(function(response) {
            // returns if the pedigree can be returned to construction mode
            var editable = response.data;

            if (!editable) {
                alertService.error({message: $.i18n.t('alerts.pedigree.editMatchError')});
            } else if ($scope.scenarios.filter(function(s) { return s.status === 'Validated';}).length > 0) {
                alertService.error({message: $.i18n.t('alerts.pedigree.editScenarioError')});

            } else if ($scope.scenarios.length > 0) {
                $modal.open({
                    templateUrl:'/assets/javascripts/pedigree/views/scenarios-delete-modal.html',
                    controller: 'scenariosDeleteModalController'
                }).result.then(function (value) {
                    if (value) {
                        changeStatus('UnderConstruction');
                        $scope.resetScenarios();
                    }
                });
            } else {
                changeStatus('UnderConstruction');
            }
        });


    };
    
    $scope.activate = function() {
        if ($scope.hasEveryUnknownParents()) {

           $modal.open({
                templateUrl: '/assets/javascripts/pedigree/views/activarPedigreeModal.html',
                controller: 'activarPedigreeModalController',
               resolve: {
                   data: function () {
                       return {
                           pedigreeName: $scope.pedigreeName,
                           freqTables: $scope.freqTables,
                           activeModels: $scope.activeModels,
                           frequencyTable: $scope.pedigree.frequencyTable,
                           mutationModelId: $scope.pedigree.mutationModelId,
                           boundary: $scope.pedigree.boundary,
                           status: $scope.pedigree.status,
                           mito: $scope.pedigree.executeScreeningMitochondrial,
                           mismatch: $scope.pedigree.numberOfMismatches,
                           isDVI:$scope.isDVI
                       };   }       }
                        }).result.then(function(response){
                                closeActivarModal(response);
                            });
      } else {
            alertService.error({message: $.i18n.t('alerts.pedigree.parentError')});
        }
    };

    function closeActivarModal(response) {
        $scope.pedigree.frequencyTable = response.frequencyTable;
        $scope.pedigree.boundary = response.boundary;
        $scope.pedigree.executeScreeningMitochondrial = response.mitocondrial;
        $scope.pedigree.numberOfMismatches = parseInt(response.mismatchMito);
        if(response.mutationModelId){
            $scope.pedigree.mutationModelId = response.mutationModelId;
        }else{
            delete $scope.pedigree.mutationModelId;
        }
        changeStatus('Active');
    }



$scope.isEditable = function() {
		return !$scope.exists || $scope.pedigree.status === 'UnderConstruction';
	};
    
    $scope.hasName = function() {
        return ($scope.$parent.pedigreeName !== undefined) &&
            ($scope.$parent.pedigreeName !== "");
    };

    $scope.isActive = function() {
        return $scope.exists && $scope.pedigree.status === 'Active';
    };

    $scope.canCreateScenarios = function() {
        return $scope.exists && ($scope.pedigree.status === 'Active');// || $scope.pedigree.status === 'Validated');
    };

    $scope.createScenario = function() {
       $scope.guardo = false;
        var nodes = getSelectedNodes();
        var scenario = {pedigreeId: $scope.pedigreeId, genogram: nodes, frequencyTable: $scope.pedigree.frequencyTable, pedigreeStatus: $scope.pedigree.status, mutationModelId: $scope.pedigree.mutationModelId};
        $scope.addScenario(scenario);
    };

    function getSelectedNodes() {
        var nodes = [];
        var selected = angular.copy($scope.pedigree.genogram.filter(function(n) { return n.selected; }));

        $scope.pedigree.genogram.forEach(function (n) {
            if(n.unknown && !n.selected){
                selected.push(angular.copy(n));
            }
        });

        var aliases = selected.map(function(n) { return n.alias; });

        selected.forEach(function(n) {
            if (aliases.indexOf(n.idFather) === -1) {
                n.idFather = undefined;
            }
            if (aliases.indexOf(n.idMother) === -1) {
                n.idMother = undefined;
            }
            if (n.unknown && !n.idFather) {
                var father = {alias: $.i18n.t('generics.fatherDash') + n.alias, sex: "Male", unknown: false};
                n.idFather = father.alias;
                nodes.push(father);
            }
            if (n.unknown && !n.idMother) {
                var mother = {alias: $.i18n.t('generics.motherDash') + n.alias, sex: "Female", unknown: false};
                n.idMother = mother.alias;
                nodes.push(mother);
            }
            nodes.push(n);
        });
        return nodes;
    }

    $scope.noSelected = function() {
      return !$scope.pedigree ||
          $scope.pedigree.genogram.filter(function(n) { return n.selected; }).length === 0;
    };

    $scope.initialize = function() {
        pedigreeService.getPedigree($scope.pedigreeId).then(function(response){
            if (response.status !== 204) {
                $scope.exists = true;
                $scope.pedigree = response.data.pedigreeGenogram;
                $scope.pedigreeMetadata = response.data.pedigreeMetaData;
                $scope.$parent.$parent.$parent.pedigreeName = $scope.pedigreeMetadata.name;
                if($scope.pedigree === undefined) {
                    $scope.pedigree = pedigreeService.defaultPedigree();
                }
                $scope.$parent.$parent.$parent.pedigreeStatus= $scope.pedigree.status;

            } else {
                $scope.exists = false;
                $scope.pedigree = pedigreeService.defaultPedigree();
                $scope.pedigreeMetadata = {};
                $scope.pedigreeMetadata.courtCaseId = Number($scope.courtcaseId);
                $scope.pedigreeMetadata.id = 0;
                $scope.pedigreeMetadata.status = $scope.pedigree.status;
                $scope.pedigreeMetadata.creationDate = new Date();
            }
            $scope.$watch('tabs[0].active', function() {
                if ($scope.tabs[0].active) {
                    $scope.drawGraph($scope.pedigree.genogram).then(function (network) {
                        pintarSelecionados(network);
                    });
                    commit();
                }
            });

            $scope.unknownIndex = $scope.getUnknownIndex();
           $scope.defaultFrequencyTable($scope.pedigree);

            if ($routeParams.u && $routeParams.p && $routeParams.m) {
                $scope.addNewScenario($routeParams.u, $routeParams.p, $routeParams.m);
            }

        }, function() {
            $scope.exists = false;
            $scope.pedigree = pedigreeService.defaultPedigree($scope.courtcaseId);
            $scope.pedigreeMetadata.courtCaseId = $scope.courtcaseId;

            $scope.drawGraph($scope.pedigree.genogram);
            commit();

            $scope.defaultFrequencyTable($scope.pedigree);
        });
        pedigreeService.getActiveMutationModels().then(function(response){
            $scope.activeModels = response.data;
        });
    };

    $timeout(function(){
        $scope.initialize();
    }, 0);

    $scope.canDelete = function(node) {
        var parentAliases = [];
        $scope.pedigree.genogram
            .forEach(function(n) {
                if (n.unknown && n.idFather) {
                    parentAliases.push(n.idFather);
                }
                if (n.unknown && n.idMother) {
                    parentAliases.push(n.idMother);
                }
            });
        return !node.unknown && (parentAliases.indexOf(node.alias) === -1);
    };

    $scope.removeNodeBoton = function () {
        if($scope.isEditable()){
        $scope.guardo = false;
        var selected = $scope.pedigree.genogram.filter(function(n) { return n.selected; });
        selected.forEach(function(n) {
            if ($scope.pedigree.genogram.some(
                    function(i) {
                        return (ng.isDefined(i.idMother) && i.idMother === n.alias) || (ng.isDefined(i.idFather) && i.idFather === n.alias);
                    })){
                alertService.error({message: $.i18n.t('alerts.node.parentError')});
                return;
            }

            if (n.unknown) {
                alertService.error({message: $.i18n.t('alerts.node.unknownError')});
                return;
            }

            for (var i = 0; i < $scope.pedigree.genogram.length; i++) {
                if($scope.pedigree.genogram[i].alias === n.alias) {
                    break;
                }
            }
            $scope.pedigree.genogram.splice(i, 1);
            $scope.drawGraph($scope.pedigree.genogram).then(function(network){
                pintarSelecionados(network);
                });
        });
        commit();
        toggleCopyButton();
        }else{
            alertService.error({message: $.i18n.t('alerts.pedigree.editModeError')});
        }
    };

        hotkeys.bindTo($scope).add({
            combo: 'ctrl+del',
            description: 'borrar',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
            callback: function () {
                $scope.removeNodeBoton();
            }
        });

    $scope.$on('selected', function(){
        if($scope.guardo){
            copiar();
        }else{
            confirmaModal();
        }});

    function copiar() {
        $scope.copiedFrom = parseInt($scope.pedigreeId);
        $scope.pedigreeMetadata.name = "";
        $scope.pedigreeMetadata.id = Number(0);
        $scope.pedigreeId = Number(0);
        $scope.pedigree._id= "0";
        $scope.pedigree.status = 'UnderConstruction';
        $scope.pedigree.genogram = getSelectedNodes();

        $scope.$parent.$parent.$parent.pedigreeName = $scope.pedigreeMetadata.name;
        $scope.drawGraph($scope.pedigree.genogram);
        $scope.changeAllSelection(false);
        commit();

        $scope.unknownIndex = $scope.getUnknownIndex();
        $scope.defaultFrequencyTable($scope.pedigree);
        $scope.exists = false;

    }

    function confirmaModal() {
         $modal.open({
            templateUrl:'/assets/javascripts/pedigree/views/copiarNodosPedigree.html',
        }).result.then(function(){
             closeModal();
        });
    }

    function closeModal() {
         copiar();
    }

    function toggleCopyButton(){
        var elegidos = $scope.noSelected();
        if (elegidos.length > 0 || !elegidos) {
            $scope.$parent.elegi(false);
        }else{
            $scope.$parent.elegi(true);
        }
    }

    function pintarSelecionados(network){
            var selected = $scope.pedigree.genogram
                .filter(function(n) { return n.selected; })
                .map(function(node) { return node.alias; });
            network.selectNodes(selected);
    }

    $scope.$on('verDetalle', function(){
            verDetalle();
        });


    function verDetalle(){
        $modal.open({
            templateUrl: '/assets/javascripts/pedigree/views/activarPedigreeModal.html',
            controller: 'activarPedigreeModalController',
            resolve: {
            data: function () {
                return {
                    pedigreeName: $scope.pedigreeName,
                    frequencyTable: $scope.pedigree.frequencyTable,
                    mutationModelId: $scope.pedigree.mutationModelId,
                    freqTables: $scope.freqTables,
                    activeModels: $scope.activeModels,
                    boundary: $scope.pedigree.boundary,
                    status: $scope.pedigree.status,
                    mito: $scope.pedigree.executeScreeningMitochondrial,
                    mismatch: $scope.pedigree.numberOfMismatches,
                    isDVI:$scope.isDVI
                };   } }
            });
                }

};

return OriginalCtrl;

});