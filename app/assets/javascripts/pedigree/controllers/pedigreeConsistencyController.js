define(['lodash','visjs/vis', 'jquery'], function(_,vis,$) {
    'use strict';
    function PedigreeConsistencyController($scope, matchesService,locusService, $routeParams,pedigreeService,$window, alertService,$timeout,$filter) {
        $scope.courtCaseId = parseInt($routeParams.courtcaseId);
        $scope.pedigreeId = parseInt($routeParams.pedigreeId);
        $scope.selectedOptions = [];
        $scope.profileSear = [];
        var network;

        locusService.listFull().then(function (response) {
            $scope.listFull= response.data;
            $scope.locusById = _.keyBy(_.map($scope.listFull, function (o) {
                return o.locus;
            }), 'id');
        });

        $scope.drawGraph = function (nodes) {
            $scope.createNetwork(nodes, "genogram", true).then(function(n){
                network = n;
                network.on('select',function () {
                    var selected = network.getSelectedNodes();
                    nodes.forEach(function (n) {
                        n.selected = selected.indexOf(n.alias) > -1;
                    });
                    if (selected.length > 0 || selected.length !== 0 ) {
                        // $scope.$parent.elegi(false);
                    }else{
                        // $scope.$parent.elegi(true);
                    }
                    $scope.$apply();
                });
                return network;
            });

        };
        $scope.initialize = function() {
            pedigreeService.getPedigree($scope.pedigreeId).then(function(response){
                if (response.status !== 204) {
                    $scope.exists = true;
                    $scope.pedigree = response.data.pedigreeGenogram;
                    $scope.pedigreeMetadata = response.data.pedigreeMetaData;
                    if($scope.pedigree === undefined) {
                        $scope.pedigree = pedigreeService.defaultPedigree();
                    }

                } else {
                    $scope.exists = false;
                    $scope.pedigree = pedigreeService.defaultPedigree();
                    $scope.pedigreeMetadata = {};
                    $scope.pedigreeMetadata.courtCaseId = Number($scope.courtcaseId);
                    $scope.pedigreeMetadata.id = 0;
                    $scope.pedigreeMetadata.status = $scope.pedigree.status;
                    $scope.pedigreeMetadata.creationDate = new Date();
                }
                $scope.drawGraph($scope.pedigree.genogram);

                // $scope.unknownIndex = $scope.getUnknownIndex();

                $scope.searchProfile();

            }, function() {
                $scope.exists = false;
                $scope.pedigree = pedigreeService.defaultPedigree($scope.courtcaseId);
                $scope.pedigreeMetadata.courtCaseId = $scope.courtcaseId;
                $scope.drawGraph($scope.pedigree.genogram);
            });

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

                        if( node.isReference === false && node.sex !== 'Unknown' ){
                            nodeArray.color = {background: '#f47521',border: '#b3572a'};
                        }
                        else{
                            if (node.sex === 'Male') {
                                nodeArray.shapeProperties = {borderRadius: 0};
                                nodeArray.color = {background: '#add8e6'};
                            } else if (node.sex === 'Female') {
                                nodeArray.color = {background: '#faafba',border: '#f778a1'};
                            } else {
                                nodeArray.color = {background: '#e5e4e2',border: '#78866b'};
                            }
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

        $scope.hasInconsistency = function(globalCode, locus) {
            var item = _.find($scope.profileSear, { 'globalCode': globalCode });
            if(!item){
                return false;
            }
            return item.locus.includes(locus);
        };
        $scope.chequearConsistencia = function(){
            $scope.isProcessing = true;
            // alertService.success({message: 'Se revisará la consistencia y recibirá una notificación cuando finalice el proceso'});
            pedigreeService.generatePedCheck($scope.pedigreeId,parseInt($scope.courtCaseId)).then(function (response) {
                if(response.data.isConsistent){
                    alertService.success({message: 'El pedigrí es consistente'});
                }else {
                    alertService.success({message: 'El pedigrí no es consistente'});
                }
                $scope.handleResponse(response);
            },function (response) {
                alertService.error({message: response.data});
            });

        };
        $scope.handleResponse= function(response){
            $scope.isProcessing = false;

            $scope.profileSear = response.data.pedigreeConsistency;
            $scope.consistencyRun = response.data.consistencyRun;
            $scope.selectedOptions = [];
            $scope.profileSear.forEach(function (x) {
                if (x && ($scope.selectedOptions.length <= 50)) {
                    var item = _.find($scope.pedigree.genogram, { 'globalCode': x.globalCode });
                    if(item){
                        x.alias = item.alias;
                    }
                    $scope.selectedOptions.push(x);
                }
            });
            var lista = _.uniq($scope.selectedOptions.map(function(element){
                return element.globalCode;
            }));
            if(lista.length>0){
                $scope.isConsistent = response.data.isConsistent;

                matchesService.getComparedMixtureGene(lista).then(function (response) {
                    $scope.comparison = response.data.filter(function (item) {
                        return $scope.locusById[item.locus] && $scope.locusById[item.locus].analysisType !== 4;
                    });
                });
            }

        };
        $scope.searchProfile= function(){
            // $scope.isProcessing = true;
                pedigreeService.getPedCheck($scope.pedigreeId,$scope.courtCaseId).then(function (response) {
                    $scope.handleResponse(response);
                });
        };
        $scope.initialize();



    }

    return PedigreeConsistencyController;
});

