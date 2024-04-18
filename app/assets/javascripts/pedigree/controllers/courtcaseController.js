define(['visjs/vis', 'jquery','lodash'], function(vis, $,_) {
'use strict';

var CourtcaseCtrl = function($scope, $filter, pedigreeService, $routeParams, $modal, statsService, alertService, $timeout,$route,$location) {
    $scope.courtcaseId = $routeParams.id;
    $scope.tabCaseData = 1;
    $scope.tabProfiles = 2;
    $scope.tabProfilesNN = 3;
    $scope.tabPedigree = 4;
    $scope.tabCollapsing = 5;
    $scope.tabComparacion = 6;
    $scope.matchesGroup = $routeParams.matchesGroup === "true";

    if($routeParams.tab){
        $scope.activeTab = parseInt($routeParams.tab);
    }else{
        $scope.activeTab = $scope.tabCaseData;
    }
    console.log('$routeParams',$routeParams);
    $scope.tabs = [{active: true},{active:false}];
    console.log('$route',$route);
    console.log('$location',$location);


    $scope.options = {};
    $scope.selectTab = function(tab){
        if(_.isUndefined($scope.courtcaseId)){
            return;
        }
        $scope.activeTab = tab;
        $location.search('tab', $scope.activeTab);
    };

    $scope.updateOptions = function(pedigree) {
        pedigree.genogram.forEach(function (individual){
            if (individual.globalCode){
                $scope.options[individual.globalCode] = individual;
            }
        });
    };

    pedigreeService.getCourtCaseFull($scope.courtcaseId).then(function(response){
        $scope.courtcase = response.data;
    });

    var loadScenario = function() {
        if ($routeParams.s) {
            var scenarios = $scope.scenarios.filter(function(scenario){return scenario.name === $routeParams.s;});
            if (scenarios.length > 0) {
                var index = $scope.scenarios.indexOf(scenarios[0]);
                $scope.tabs[index+2].active = true;
            }
        } 
    };
    
    pedigreeService.getScenarios($scope.courtcaseId).then(function(response) {
        $scope.scenarios = response.data;
        $scope.scenarios.forEach(function(scenario) {
            $scope.tabs.push({active:false});
            $scope.updateOptions(scenario);
        });

        loadScenario();
    });

    $scope.addScenario = function(scenario) {
        $modal.open({
            templateUrl:'/assets/javascripts/pedigree/views/scenario-name-modal.html',
            controller: 'scenarioNameModalController'
        }).result.then(function (response) {
            if (response.ok) {
                scenario.name = response.name;
                scenario.description = response.description;
                $scope.scenarios.push(scenario);
                $scope.tabs.push({active:true});
            }
        });

    };

    $scope.isMale = function(node) {
        return node.sex === 'Male';
    };

    $scope.isFemale = function(node) {
        return node.sex === 'Female';
    };

    $scope.sex = pedigreeService.getAvailableSex();

    $scope.resetScenarios = function() {
        $scope.scenarios = [];
    };

    $scope.createNetwork = function(nodes, containerDiv, selectable) {
        return $timeout(function() {
            var shapes = {
                'Unknown': 'diamond',
                'Male': 'square',
                'Female': 'circle'
            };

            var nodesArray = [];
            var edgesArray = [];
            nodes.forEach(function (node) {
                var nodeArray = {
                    id: node.alias,
                    label: $filter('prittyLimitTo')(node.alias,5),
                    shape: shapes[node.sex],
                    title: node.alias
                };

                if (node.globalCode) {
                    if (node.sex === 'Male') {
                        nodeArray.shapeProperties = {borderRadius: 0};
                        nodeArray.color = {background: '#add8e6'};
                    } else if (node.sex === 'Female') {
                        nodeArray.color = {background: '#faafba',border: '#f778a1'};
                    } else {
                        nodeArray.color = {background: '#e5e4e2',border: '#78866b'};
                    }
                } else {
                    nodeArray.color = {background: '#e5e4e2',border: '#78866b'};
                }

                nodesArray.push(nodeArray);

                if (node.idFather) {
                    edgesArray.push({
                        from: node.alias,
                        to: node.idFather,
                        title: node.idFather + ' padre de ' + node.alias
                    });
                }

                if (node.idMother) {
                    edgesArray.push({
                        from: node.alias,
                        to: node.idMother,
                        title: node.idMother + ' madre de ' + node.alias
                    });
                }
            });

            var nodesData = new vis.DataSet(nodesArray);
            var edgesData = new vis.DataSet(edgesArray);

            var container = $("#" + containerDiv).get(0);

            var data = {
                nodes: nodesData,
                edges: edgesData
            };

            var options = {
                interaction: {
                    navigationButtons: true,
                    multiselect: selectable
                },
                nodes: {
                    fixed: false,
                    color: {
                        highlight: '#b66c9f'
                    }
                },
                edges: {
                    color: {
                        color: 'grey',
                        highlight: '#b66c9f'
                    },
                    arrows: {
                        to: {enabled: false,scaleFactor: 0},
                        middle: {enabled: false,scaleFactor: 0},
                        from: {enabled: true,scaleFactor: 0.5}
                    },
                    smooth: {
                        enabled: true,
                        type: 'cubicBezier',
                        roundness: 1,
                        forceDirection: 'vertical'
                    }
                },
                layout: {
                    randomSeed: undefined,
                    improvedLayout: true,
                    hierarchical: {
                        enabled: true,
                        levelSeparation: 150,
                        direction: 'DU',
                        sortMethod: 'directed',
                        parentCentralization: true,
                        edgeMinimization: true,
                        blockShifting: true,
                        treeSpacing: 50,
                        nodeSpacing: 200
                    }
                },
                autoResize: true,
                width: '100%',
                height: '100%',
                physics: false
            };

            var network = new vis.Network(container,data,options);
            network.fit();
            return network;
        }, 0);
    };

    $scope.defaultFrequencyTable = function(pedigree) {
        if (!$scope.freqTables){
            statsService.getActiveTables().then(function(tables) {
                $scope.freqTables = tables;

                for(var name in $scope.freqTables) {
                    if ($scope.freqTables.hasOwnProperty(name) && $scope.freqTables[name].default && !pedigree.frequencyTable) {
                        pedigree.frequencyTable = name;
                    }
                }

                if (!pedigree.frequencyTable) {
                    alertService.info({message: 'No existe ninguna base de frecuencias seleccionada o por default. Por favor, seleccione una base de frecuencias.'});
                }
            });
        }
    };
    
    $scope.validatePedigree = function() {
        $scope.validated = true;
    };
    
};

return CourtcaseCtrl;

});