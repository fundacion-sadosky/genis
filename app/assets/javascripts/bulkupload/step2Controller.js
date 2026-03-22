define(['jquery', 'lodash'], function($, _) {
    'use strict';

    function Step2Controller($scope, $routeParams, $timeout, bulkuploadService, helper, $log, $modal, alertService, $q, userService, locusService, profiledataService, notificationsService, matchesService, profileService, superiorInstanceService) {

        $scope.statusMap = bulkuploadService.getStatusMap();
        var toogle = true;
        var user = userService.getUser();

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        var $modalInstance = {};
        $scope.shared = { profileId: 0, matches: {}, profileData: [] };
        $scope.editedSubcats = {};
        $scope.batches = [];

        $scope.batchesPage = 1;
        $scope.batchesPageSize = 10;
        $scope.totalBatches = 0;

        $scope.protoProfiles = {};
        $scope.showOptions = [{label: 'Pendientes', filters: {pending: true}, placement: 'bottom'},
            {label: 'Todo', filters: {}, placement: 'top'}];
        $scope.activeOption = 1;

        var subcategoryPromise = bulkuploadService.getSubcategories().then(function(response){
            $scope.subcategory = response.data;
            return response.data;
        });
        profiledataService.getCategories().then(function(response) {
            $scope.categories = response.data;
        });

        $scope.loadBatchesCount = function () {
            bulkuploadService.countBatchesStep2(user.geneMapperId).then(function (response) {
                $scope.totalBatches = response.data.total;
            });
        };

        $scope.getIsProfileReplicatedInternalCode = function(internalCode) {
            return profiledataService.getIsProfileReplicatedInternalCode(internalCode)
                .then(function(response) {
                    return !!response.data; // Convert to boolean
                })
                .catch(function(error) {
                    console.error("Error checking replication:", error);
                    return false; // Handle errors gracefully
                });
        };

        function createProfileUpdateHandler(subcategory, getIsProfileReplicatedInternalCode) {
            return function(profile) {
                // run both async checks in parallel
                return Promise.all([
                    getIsProfileReplicatedInternalCode(profile.sampleName),
                    profiledataService.getIsProfileReplicableInternalCode(profile.sampleName)  // Use directly
                ]).then(function(results) {
                    var isReplicated = !!results[0];
                    var isReplicable = !!results[1];
                    profile.replicated = isReplicated;
                    profile.replicable = isReplicable;

                    if(profile.status === 'Imported' && !isReplicable) {
                        profile.status = 'ReplicatedMatchingProfile';
                    }

                    profile.replicateDisabled =
                        !subcategory[profile.category].replicate ||
                        profile.replicated ||
                        !profile.replicable ||
                        profile.status !== 'Imported' ||
                        profile.status === 'ReplicatedMatchingProfile';

                    if (profile.replicateDisabled) {
                        profile.replicate = false;
                    }

                    _.forEach(
                        profile.genotypification,
                        _.partial(bulkuploadService.fillRange, $scope.locusById, locusService.isOutOfLadder)
                    );
                    return profile;
                }).catch(function() {
                    // on error assume not replicable and keep existing replicated value handling
                    profile.replicable = false;
                    profile.replicateDisabled =
                        !subcategory[profile.category].replicate ||
                        profile.replicated ||
                        !profile.replicable ||
                        profile.status !== 'Imported';
                    return profile;
                });
            };
        }


        var getBatchProtoProfiles = function(batch) {
            batch.isProcessing = true;
            return bulkuploadService.getProtoProfilesStep2(user.geneMapperId, batch.id, batch.page - 1, batch.pageSize)
                .then(function(response) {
                    var profileUpdatePromises = [];
                    var profileUpdateHandler = createProfileUpdateHandler($scope.subcategory, $scope.getIsProfileReplicatedInternalCode);

                    for (var i = 0; i < response.data.length; i++) {
                        profileUpdatePromises.push(profileUpdateHandler(response.data[i]));
                    }

                    return $q.all(profileUpdatePromises)
                        .then(function(updatedProfiles) {
                            $scope.protoProfiles[batch.id] = updatedProfiles;
                            $scope.protoProfiles[batch.id].allReplicated = false;
                            $scope.protoProfiles[batch.id].anyReplicable = false;
                            aprobados(batch.id);
                            batch.isProcessing = false;
                            updateAllReplicatedStatus(batch.id);
                            updateDisableReplicateAllStatus(batch.id);
                        });
                })
                .catch(function() {
                    batch.isProcessing = false;
                });
        };


        var refreshSingleProfile = function(batch, profileId) {
            batch.isProcessing = true;
            return bulkuploadService.getProtoProfileBySampleId(profileId)
                .then(function(response) {
                    var profile = response.data;
                    var profileUpdateHandler = createProfileUpdateHandler(
                        $scope.subcategory,
                        $scope.getIsProfileReplicatedInternalCode
                    );
                    return profileUpdateHandler(profile).then(function(updatedProfile) {
                        $scope.protoProfiles[batch.id] = [updatedProfile];
                        $scope.protoProfiles[batch.id].allReplicated = false;
                        $scope.protoProfiles[batch.id].anyReplicable = false;
                        aprobados(batch.id);
                        batch.isProcessing = false;
                        updateAllReplicatedStatus(batch.id);
                        updateDisableReplicateAllStatus(batch.id);
                    });
                })
                .catch(function() {
                    batch.isProcessing = false;
                });
        };

        var loadLocus = function() {
            return locusService.listFull().then(function(response) {
                $scope.locus = response.data;
                $scope.locusById = _.keyBy(_.map($scope.locus, function (o) {  return o.locus;}), 'id');
            });
        };

        var getAllBatches = function() {
            $scope.isProcessing = true;
            return bulkuploadService.getBatchesStep2(user.geneMapperId, $scope.batchesPage, $scope.batchesPageSize).then(function(response) {
                if ($scope.activeOption === 0) {
                    $scope.batches = response.data.filter(function(t) {
                        return t.totalForApprovalOrImport !== 0;
                    });
                } else {
                    $scope.batches = response.data;
                }

                $scope.batches.forEach(function(batch) {
                    batch.pageSize = 50;
                    batch.page = 1;
                    var isOpen = false;
                    batch.replicateSelectedDisabled = true;

                    Object.defineProperty(batch, "isOpen", {
                        get: function() { return isOpen; },
                        set: function(newValue) {
                            isOpen = newValue;
                            if (isOpen) {
                                getBatchProtoProfiles(batch);
                            }
                        }
                    });
                });
                $scope.isProcessing = false;
            });
        };
        
        $scope.changeBatchesPage = function () {
            getAllBatches();
        };

        var getBatchItem = function(id, batch) {
            if (batch){
                batch.isProcessing = true;
            }

            bulkuploadService.getProtoProfileBySampleId(id).then(function(response) {
                if (batch && batch.id) {
                    $scope.protoProfiles[batch.id] = $scope.protoProfiles[batch.id].map(function(x){
                        return (x.id === id)? response.data: x;
                    });
                } else {
                    $scope.protoProfiles = helper.groupBy([response.data], 'batchId');
                    $scope.batches = Object.keys($scope.protoProfiles).map(function(id) {
                        return {id: id, isOpen: true};
                    });
                }
                if (batch){
                    batch.isProcessing = false;
                }
            }, function() { if (batch) { batch.isProcessing = false; } });
        };

        $scope.protoprofileId = $routeParams.protoprofileId;
        if ($scope.protoprofileId) {
            // Cuando se entra desde una notificación: cargar locus y subcategorías primero,
            // luego el perfil y el total real del lote para habilitar correctamente los checkboxes.
            $q.all([subcategoryPromise, loadLocus()]).then(function() {
                return bulkuploadService.getProtoProfileBySampleId($scope.protoprofileId)
                    .then(function(response) {
                        var profile = response.data;
                        var batchId = profile.batchId;

                        // Obtener el total absoluto del lote (sin filtros de estado ni asignación)
                        // para determinar correctamente si se habilita la búsqueda de escritorio.
                        return bulkuploadService.countAllProtoProfilesInBatch(batchId)
                            .then(function(countResponse) {
                                var totalInBatch = countResponse.data.total;

                                var batch = {
                                    id: batchId,
                                    pageSize: 50,
                                    page: 1,
                                    replicateSelectedDisabled: true,
                                    totalAnalysis: totalInBatch,
                                    batchTotal: totalInBatch,
                                    singleProfileId: $scope.protoprofileId
                                };
                                $scope.batches = [batch];

                                var profileUpdateHandler = createProfileUpdateHandler(
                                    $scope.subcategory,
                                    $scope.getIsProfileReplicatedInternalCode
                                );
                                return profileUpdateHandler(profile).then(function(updatedProfile) {
                                    $scope.protoProfiles[batchId] = [updatedProfile];
                                    $scope.protoProfiles[batchId].allReplicated = false;
                                    $scope.protoProfiles[batchId].anyReplicable = false;
                                    aprobados(batchId);
                                    updateAllReplicatedStatus(batchId);
                                    updateDisableReplicateAllStatus(batchId);
                                });
                            });
                    });
            });
        } else {
            getAllBatches();
            loadLocus();
        }

        var updateStatus = function(sample, status, batch) {
            var id = parseInt(sample.id, 10);
            batch.isProcessing = true;

            var isDesktopSearchForThisProfile =
                batch.desktopSearch &&
                $scope.protoProfiles[batch.id] &&
                $scope.protoProfiles[batch.id].length === 1 &&
                sample.id === $scope.protoProfiles[batch.id][0].id;

            return bulkuploadService
                .changeStatus(id, status, sample.replicate, isDesktopSearchForThisProfile)
                .then(function(response) {
                    if (response.data && response.data.length > 0) {
                        // Backend devolvió errores
                        if (batch.id) {
                            $scope.protoProfiles[batch.id].forEach(function(b) {
                                if (b.id === id) {
                                    b.errors = response.data;
                                }
                            });
                        } else {
                            $scope.protoProfiles[0].errors = response.data;
                        }
                    }

                    // Siempre recalculamos “todosAprobados” con los datos actuales
                    aprobados(batch.id);

                    // Recargamos el batch. Si venimos de una notificación, refrescamos
                    // sólo el perfil individual; si no, recargamos todo el batch.
                    var refreshFn = batch.singleProfileId
                        ? function() { return refreshSingleProfile(batch, batch.singleProfileId); }
                        : function() { return getBatchProtoProfiles(batch); };
                    return refreshFn().finally(function() {
                        batch.isProcessing = false;
                    });

                }, function(response) {
                    batch.isProcessing = false;
                    alertService.error({message: ' Hubo un error: ' + response.data});
                    $log.log(response);
                });
        };



        var rejectPp = function(sample, motive, batch, idMotive) {
            batch.isProcessing = true;
            bulkuploadService.rejectProtoProfile(sample.id, motive, idMotive).then(function(response) {
                    if (response.data.length > 0) {
                        if (batch) {
                            $scope.protoProfiles[batch.id].forEach(function(b){
                                if (b.id === sample.id) {b.errors = response.data;}
                            });
                        } else { $scope.protoProfiles[0].errors = response.data; }
                    } else {
                        sample.status = 'Rejected';
                        sample.selected = false;
                    }
                    aprobados(batch.id);
                    batch.isProcessing = false;
                },
                function(response){
                    batch.isProcessing = false;
                    alertService.error({message: ' Hubo un error: ' + response.data});
                    $log.log(response);
                });
        };

        $scope.import = function(r, batch) {
            updateStatus(r, 'Imported', batch);
        };

        $scope.onChangeMotive = function(selectedMotive) {
            $scope.showMotiveTextArea = false;
            for (var i = 0; i < $scope.motives.length; i++) {
                if ($scope.motives[i].id === parseInt(selectedMotive) && $scope.motives[i].freeText) {
                    $scope.showMotiveTextArea = true;
                    $scope.motiveText = "";
                }
            }
        };

        $scope.reject = function(sample, batchId) {
            $scope.showMotiveTextArea = false;
            bulkuploadService.getMotives().then(function(response) {
                $scope.motives = response.data;
            }, function() {
                $scope.motives = [];
            });
            $modalInstance = $modal.open({
                templateUrl: '/assets/javascripts/bulkupload/modalRejectProtoProfile.html',
                scope: $scope
            });

            $modalInstance.result.then(function(res){
                rejectPp(sample, res.motive, batchId, res.idMotive);
            });
        };

        $scope.closeModal = function(motiveText, selectedMotive) {
            if (motiveText === false) {
                $modalInstance.dismiss('cancel');
            } else {
                for (var i = 0; i < $scope.motives.length; i++) {
                    if ($scope.motives[i].id === parseInt(selectedMotive) && !$scope.motives[i].freeText) {
                        motiveText = $scope.motives[i].description;
                    }
                }
                $modalInstance.close({motive: motiveText, idMotive: selectedMotive});
            }
        };

        $scope.importSelected = function(batch) {
            var promises = [];
            $scope.protoProfiles[batch.id].forEach(function(sample) {
                if (sample.selected) {
                    promises.push(updateStatus(sample, 'Imported', batch).then(function() {
                        sample.selected = false;
                    }));
                }
            });
            $q.all(promises).then(function() {
                alertService.success({message: 'Se ha realizado el cambio de estado satisfactoriamente'});
            }, function(response) {
                alertService.error({message: ' Hubo un error: ' + response.toString()});
            });
        };

        notificationsService.onNotification(function(msg){
            if (msg.kind === 'matching') {
                $timeout(() => {}, 1500);

                $scope.shared.profileId = msg.info.globalCode;
                console.debug("Profile defined on notification: ", $scope.shared.profileId);
                $scope.shared.matches[msg.info.matchedProfile] = msg.info.matchingId;
                console.debug("Profile match defined on notification: ", msg.info.matchedProfile);

                profiledataService.getProfileDataBySampleCode($scope.shared.profileId).then(function(response) {
                    $scope.shared.profileData = response.data;
                });

            }
        });


        $scope.removeDesktopProfiles = function() {
            profiledataService.getDesktopProfiles().then(function(profiles) {
                console.debug("removing desktop profiles: ", profiles.data);
                profiles.data.forEach(function(profile) {
                    profileService.removeProfile(profile).then(function () {
                        console.log("Desktop profile removed:", profile);
                    });

                    profiledataService.removeProfile(profile).then(function () {
                        console.log("Desktop profile data removed:", profile);
                    });
                });
            });
        };

        function closeDesktopResults(){
            console.debug("Closing from step2controller");

            if ($scope.shared.profileId === 0){ // if not defined, remove all desktop profiles
                $scope.removeDesktopProfiles();
                console.log("All desktop profiles removed");
            } else {
                //remove profile (mongoDB)
                profileService.removeProfile($scope.shared.profileId).then(function() {
                    console.log("Profile removed:", $scope.shared.profileId);
                });

                //remove profile data
                profiledataService.removeProfile($scope.shared.profileId).then(function() {
                    console.log("Profile data removed:", $scope.shared.profileId);
                });
            }
        }

        $scope.desktopSearchResults = function(batch) {
            $scope.fromDesktopSearch = true;
            if (batch.desktopSearch) {
                $modal.open({
                    templateUrl: '/assets/javascripts/bulkupload/desktopSearchResults.html',
                    controller: 'desktopSearchReportController',
                    resolve: {
                        shared: function() {
                            return $scope.shared;
                        }
                    }
                }).result.then(
                    function onClose() {
                        // cerró con modalInstance.close()
                        console.debug("Closing from step2controller (close)");
                        console.debug("On close, profileId:", $scope.shared.profileId);
                        closeDesktopResults();
                    },
                    function onDismiss(reason) {
                        // cerró por clic fuera, ESC o modalInstance.dismiss()
                        console.debug("Closing from step2controller (dismiss):", reason);
                        console.debug("On close, profileId:", $scope.shared.profileId);
                        closeDesktopResults();
                    });
            }
        };

        $scope.importBatch = function(batch) {
            $scope.matches = {};
            batch.isProcessing = true;
            var protoprofilesFromBatch = $scope.protoProfiles[batch.id];
            var promises = [];

            if (!_.isUndefined(protoprofilesFromBatch)) {
                protoprofilesFromBatch.forEach(function(sample) {
                    // MODIFIED: Only process profiles that have the status 'Approved'
                    if (sample.status === 'Approved') {
                        // updateStatus handles the changeStatus API call, the replication flag, and the desktopSearch flag per item
                        promises.push(updateStatus(sample, 'Imported', batch));
                    }
                });
            }

            // Wait for all individual updates to complete
            $q.all(promises).then(function() {
                batch.isProcessing = false;
                getBatchProtoProfiles(batch); // Refresh the list from backend
                alertService.success({message: 'Se han importado los perfiles exitosamente.'});
            }, function(response) {
                batch.isProcessing = false;
                getBatchProtoProfiles(batch);
                // Individual errors are usually handled inside updateStatus via alertService
            });
        };

        $scope.deleteBatch = function(batch) {
            $scope.isProcessing = true;
            bulkuploadService.deleteBatch(batch.id).then(function() {
                $scope.isProcessing = false;
                getAllBatches();
                alertService.success({message: 'Se ha eliminado el lote.'});
            }, function(response) {
                $scope.isProcessing = false;
                alertService.error({message: ' Hubo un error: ' + response.data.message});
            });
        };

        $scope.doDeleteBatch = function(confirmRes, batch) {
            if (!confirmRes) {
                return;
            }
            $scope.deleteBatch(JSON.parse(batch));
        };

        $scope.selectAll = function(batchId) {
            bulkuploadService.selectAll(batchId, $scope.protoProfiles[batchId], toogle, 'Approved');
            toogle = !toogle;
        };

        $scope.setMismatch = function(id, batchId) {
            var pp = $scope.protoProfiles[batchId].filter(function(x){ return x.id === id; })[0];
            var strigencyModalInstance = $modal.open({
                templateUrl: '/assets/javascripts/profiles/views/stringency-modal.html',
                controller: 'stringencyModalController',
                resolve: {
                    data: function() {
                        return {
                            subcategory: {id: pp.category},
                            profile: pp.sampleName,
                            subcatsRel: pp.matchingRules,
                            mismatches: pp.mismatches
                        };
                    }
                }
            });

            strigencyModalInstance.result.then(function(ret) {
                $scope.subcatsRel = ret.subcatsRel;
                $scope.mismatches = ret.mismatches;

                bulkuploadService.updateProtoProfileRulesMismatch(id, ret.subcatsRel, ret.mismatches).then(function(response) {
                    if (response.data === id) {
                        getBatchItem(id, batchId);
                    } else {
                        alertService.error({message: ' Hubo un error: ' + response.data});
                    }
                });
            }, function(){});
        };

        $scope.editData = function(sample, action, batchId) {
            if (action === 'edit') { $scope.edit = sample.id; }
            if (action === 'cancel') { $scope.edit = ''; }

            if (action === 'save') {
                bulkuploadService.updateProtoProfileData(sample.id, $scope.editedSubcats[sample.id]).then(function(response) {
                    if (response.data.error) {
                        alertService.error({message: response.data.error});
                    } else {
                        response.data.replicate = $scope.subcategory[response.data.category].replicate;
                        response.data.replicateDisabled = !$scope.subcategory[response.data.category].replicate;
                        if ($scope.subcategory[response.data.category].replicate) {
                            response.data.replicate = false;
                            response.data.replicateDisabled = false;
                        } else {
                            response.data.replicate = false;
                            response.data.replicateDisabled = true;
                        }
                        $scope.protoProfiles[batchId] = $scope.protoProfiles[batchId].map(function(x){
                            return (x.id === sample.id)? response.data: x;
                        });
                    }
                    $scope.edit = '';
                }, function(response) {
                    alertService.error({message: 'Hubo un error: ' + response.data.message});
                });
            }
        };

        var aprobados = function(batchId) {
            $scope.protoProfiles[batchId].todosAprobados = true;

            $scope.protoProfiles[batchId].forEach(function(n) {
                if (n.status === 'Approved') {
                    $scope.protoProfiles[batchId].todosAprobados = false;
                }
            });
        };

        $scope.show = function(index) {
            $scope.activeOption = index;
            getAllBatches();
        };

        $scope.handleReplicateChange = function(batch) {
            if (batch.replicate) {
                // Uncheck and disable "Busqueda de Escritorio"
                batch.desktopSearch = false;
                batch.desktopSearchDisabled = true;
            } else {
                // Enable "Busqueda de Escritorio"
                batch.desktopSearchDisabled = false;
            }

            // Update the state of the "Replicar Seleccionados" button
            batch.replicateSelectedDisabled = !$scope.anySelectedForReplication(batch.id);
        };

        $scope.handleDesktopSearchChange = function(batch) {
            // No actions needed here, the UI handles the disabled state correctly
        };

        $scope.replicateAllToggle = function(batchId) {
            var allReplicated = $scope.protoProfiles[batchId].allReplicated;

            _.forEach($scope.protoProfiles[batchId], function(r) {
                if (!r.replicateDisabled && r.status === 'Imported') {
                    r.replicate = !allReplicated;
                }
            });

            updateAllReplicatedStatus(batchId);
            var batch = _.find($scope.batches, {id: batchId});
            batch.replicateSelectedDisabled = !$scope.anySelectedForReplication(batchId);
        };


        function updateAllReplicatedStatus(batchId) {
            var anyReplicable = false;
            var allReplicated = true;

            _.forEach($scope.protoProfiles[batchId], function(r) {
                if (!r.replicateDisabled && r.status === 'Imported') {
                    anyReplicable = true;
                    if (!r.replicate) {
                        allReplicated = false;
                    }
                }
            });

            $scope.protoProfiles[batchId].allReplicated = allReplicated && anyReplicable;
            $scope.protoProfiles[batchId].anyReplicable = anyReplicable;
        }


        $scope.replicateSelected = function(batchId) {
            var batch = _.find($scope.batches, {id: batchId});
            batch.isProcessing = true;

            // Collect IDs of profiles that are selected, have "Replicar a Instancia Superior" checked, and are Accepted
            var idsToReplicate = [];
            _.forEach($scope.protoProfiles[batchId], function(r) {
                if (r.replicate && !r.replicateDisabled && r.status === 'Imported') {
                    idsToReplicate.push(r.id);
                }
            });
            // Acá podría hacer el upload de los globalcodes de los idsToReplicate para que el backend los replique
            // llamando a profileService.uploadProfilesForReplication(idsToReplicate) o algo así
            // Call the service to replicate the selected profiles
            bulkuploadService.changeBatchStatus(batchId, 'Uploaded', idsToReplicate, false ) // uploaded es un nuevo estado en ProtoProfileStatus.scala replicateAll = false, since we only want to replicate selected
                .then(function() {
                    batch.isProcessing = false;  // Re-enable buttons
                    getBatchProtoProfiles(batch); // Refresh the data
                    alertService.success({message: 'Se han replicado los perfiles seleccionados.'});
                }, function(response) {
                    batch.isProcessing = false; // Re-enable buttons
                    getBatchProtoProfiles(batch); // Refresh the data
                    alertService.error({message: ' Hubo un error: ' + response.data});
                });
        };

        //This is the function that enable or disable the Replicar Seleccionados button
        $scope.anySelectedForReplication = function(batchId) {
            if (!$scope.protoProfiles[batchId]) {
                return false; // Or true depending on the initial state you want
            }
            return _.some($scope.protoProfiles[batchId], function(r) {
                console.log(r);
                return r.replicate && !r.replicateDisabled && r.status === 'Imported';
            });
        };

        function updateDisableReplicateAllStatus(batchId) {
            $scope.protoProfiles[batchId].disableReplicateAll = !_.some(
                $scope.protoProfiles[batchId],
                function (r) {
                    return !r.replicateDisabled && r.status === 'Imported';
                }
            );
        }


        // Watch for changes in selected checkboxes and replicate checkboxes, and update the "Replicar seleccionados" button state
        $scope.$watch(function() {
            if (!$scope.protoProfiles) {
                return {};
            }
            _.forEach($scope.protoProfiles, function(profiles, batchId) {
                updateDisableReplicateAllStatus(batchId);
            });

            return $scope.protoProfiles;
        }, true);

        $scope.loadBatchesCount();
    }

    return Step2Controller;
});