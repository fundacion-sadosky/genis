define(['lodash'], function(_) {
'use strict';

var PedigreeService = function(playRoutes, userService, $http) {
	
	this.getCaseTypes = function () {
        return $http.get('/pedigree/caseTypes');
        //return playRoutes.controllers.Pedigrees.getCaseTypes().get();
    };
	
	this.getAvailableSex = function(){
		return ['Female', 'Male', 'Unknown'];
	};
	
	this.defaultPedigree = function() {
		return {
			_id: "0",
			genogram: [{alias: "PI1", sex: 'Unknown', unknown: true, idMother: "Madre", idFather: "Padre"},
				{alias: "Madre", sex: 'Female', unknown: false},
				{alias: "Padre", sex: 'Male', unknown: false}],
			status: 'UnderConstruction',
			boundary: 1000
		};
	};
	
	this.getTotalCourtCases = function(search){
		var user = userService.getUser();
        search.user = user.name;
        search.isSuperUser = user.superuser;
        console.log('GET TOTAL COURT CASES');
        return $http.post('/pedigree/total-court-cases', search);
		//return playRoutes.controllers.Pedigrees.getTotalCourtCases().post(search);
	};

	this.getCourtCases = function(search){
        var user = userService.getUser();
        search.user = user.name;
        search.isSuperUser = user.superuser;
        console.log('GET COURT CASES');
        return $http.post('/pedigree/court-cases', search);
		//return playRoutes.controllers.Pedigrees.getCourtCases().post(search);
	};
	
	this.getPedigreesByCourtCase = function(id){
        return $http.get('/pedigree/by/' + id);
		//return playRoutes.controllers.Pedigrees.getByCourtCase(id).get();
	};
	
	this.createCourtCase = function(courtCase){
		var user = userService.getUser();
		courtCase.assignee = user.name;
        console.log('CREATE COURT CASES');
        return $http.post('/pedigree/create-court-cases', courtCase);
		//return playRoutes.controllers.Pedigrees.createCourtCase().post(courtCase);
	};
	
	this.updateCourtCase = function(courtCase){
		var user = userService.getUser();
		courtCase.assignee = user.name;
        return $http.put('/pedigree/court-cases/' + courtCase.id, courtCase);
		//return playRoutes.controllers.Pedigrees.updateCourtCase(courtCase.id).put(courtCase);
	};

	this.createPedigreeMetadata = function(pedigreeMetadata) {
        console.log('CREATE PEDIGREE METADATA');
        return $http.post('/pedigree/save2', pedigreeMetadata);
		//return playRoutes.controllers.Pedigrees.createOrUpdatePedigreeMetadata().post(pedigreeMetadata);
	};
	this.createPedigree = function(pedigree) {
        console.log('CREATE GENOGRAM');
        return $http.post('/pedigree/create-genogram', pedigree);
		//return playRoutes.controllers.Pedigrees.createGenogram().post(pedigree);
	};

	this.createCompletePedigree = function(pedigreeData) {
        console.log('CREATE PEDIGREE');
        return $http.post('/pedigree/save', pedigreeData);
		//return playRoutes.controllers.Pedigrees.createPedigree().post(pedigreeData);
	};

	this.getPedigree = function(pedigreeId) {
        return $http.get('/pedigree/genogram/' + pedigreeId);
		//return playRoutes.controllers.Pedigrees.getPedigree(pedigreeId).get();
	};
	
	this.getCourtCaseFull = function(courtcaseId) {
        return $http.get('/pedigree/full/' + courtcaseId);
		//return playRoutes.controllers.Pedigrees.getCourtCaseFull(courtcaseId).get();
	};

    this.getCourtCaseBy = function(courtcaseId) {
        return playRoutes.controllers.Pedigrees.getCourtCaseBy(courtcaseId).get();
    };

	this.search = function(input) {
        return $http.get('/search/profileDataForPedigree', { params: { input: input } });
		//return playRoutes.controllers.SearchProfileDatas.searchProfilesForPedigree(input).get();
	};
	
	this.changePedigreeStatus = function(id, status, genogram) {
        genogram.genogram.forEach(function(item){delete item.$$hashKey; return;});
        if(!_.isUndefined(genogram.mutationModelId)){
            genogram.mutationModelId = genogram.mutationModelId.toString();
		}
        console.log('CHANGE PEDIGREE STATUS');
        return $http.post('/pedigree/status', genogram, { params: { id: id, status: status } });
		//return playRoutes.controllers.Pedigrees.changePedigreeStatus(id, status).post(genogram);
	};

	this.changeStatus = function(id, status, closeProfiles,courtCase) {
        console.log('CHANGE COURT CASE STATUS');
        return $http.post('/pedigree/courtCaseStatus', courtCase, {
            params: {
                id: id,
                status: status,
                closeProfiles: closeProfiles
            }
        });
		//return playRoutes.controllers.Pedigrees.changeCourtCaseStatus(id, status, closeProfiles).post(courtCase);
	};

	this.canEdit = function(pedigreeId) {
        return $http.get('/pedigree/editable/' + pedigreeId);
		//return playRoutes.controllers.Pedigrees.canEdit(pedigreeId).get();	
	};

	this.canDelete = function(pedigreeId) {
        return $http.get('/pedigree/deleteable/' + pedigreeId);
		//return playRoutes.controllers.Pedigrees.canDelete(pedigreeId).get();
	};

	this.canDeleteCourtCase = function(courtCaseId) {
        return $http.get('/court-case/deleteable/' + courtCaseId);
		//return playRoutes.controllers.Pedigrees.canDeleteCourtCase(courtCaseId).get();
	};

	this.canCloseCourtCase = function(courtCaseId) {
        return $http.get('/court-case/closeable/' + courtCaseId);
		//return playRoutes.controllers.Pedigrees.canCloseCourtCase(courtCaseId).get();
	};
	
	this.fisicalDelete = function(pedigreeId, pedigree) {
        console.log('FISICAL DELETE PEDIGREE');
        return $http.post('/pedigree/delete', pedigree, { params: { id: pedigreeId } });
		//return playRoutes.controllers.Pedigrees.fisicalDeletePedigree(pedigreeId).post(pedigree);
	};

    this.createScenario = function(scenario) {
        if(!_.isUndefined(scenario.lr)){
            scenario.lr = scenario.lr.toString();
        }
        console.log('CREATE SCENARIO');
        return $http.post('/pedigree/scenario', scenario);
        //return playRoutes.controllers.Pedigrees.createScenario().post(scenario);
    };

    this.updateScenario = function(scenario) {
		if(!_.isUndefined(scenario.lr)){
            scenario.lr = scenario.lr.toString();
		}
        return $http.put('/pedigree/scenario', scenario);
        //return playRoutes.controllers.Pedigrees.updateScenario().put(scenario);
    };
    
    this.changeScenarioStatus = function(scenario, status) {
		if(!_.isUndefined(scenario.lr)){
            scenario.lr = scenario.lr.toString();
        }
        console.log('CHANGE SCENARIO STATUS');
        return $http.post('/pedigree/scenario-status', scenario, {
            headers: { 'Scenario-Status': status }
        });
        //return playRoutes.controllers.Pedigrees.changeScenarioStatus(status).post(scenario);
    };

	this.confirmScenario = function(scenario, status, pedigreeActivo) {
        if(!_.isUndefined(scenario.lr)){
            scenario.lr = scenario.lr.toString();
        }
        console.log('CONFIRM SCENARIO');
        return $http.post('/pedigree/scenario-validate', {
            scenario: scenario,
            status: status,
            pedigreeActivo: pedigreeActivo
        });
		//return playRoutes.controllers.Pedigrees.confirmEscenarioScenario(status, pedigreeActivo).post(scenario);
	};
    
    this.getScenarios = function(pedigreeId) {
        return $http.get('/pedigree/scenarios/' + encodeURIComponent(pedigreeId));
        //return playRoutes.controllers.Pedigrees.getScenarios(pedigreeId).get();  
    };

	this.getScenario = function(scenarioId) {
        return $http.get('/pedigree/scenario/' + encodeURIComponent(scenarioId));
		//return playRoutes.controllers.Pedigrees.getScenario(scenarioId).get();
	};
	
	this.getLR = function(scenario) {
        console.log('GET LR');
        return $http.post('/pedigree/lr', scenario);
		//return playRoutes.controllers.Pedigrees.getLR().post(scenario);
	};

	this.getSubCatName = function(groups, catId){
		for ( var grpId in groups) {
			var categories = groups[grpId].subcategories;
			for (var i = 0; i < categories.length; i++) {
				var category = categories[i];
				if(category.id === catId) {
					return groups[grpId].name  + " / " + category.name;
				}
			}
		}
	};

    this.getProfiles = function(search,tab) {
        var params = {
            id: search.idCourtCase,
            pageSize: search.pageSize,
            page: search.page,
            input: search.searchText,
            isReference: tab,
            statusProfile: search.statusProfile
        };    
        return $http.get('/court-case-profiles', { params: params });
        //return playRoutes.controllers.Pedigrees.getProfiles(search.idCourtCase,search.pageSize,search.page,search.searchText, tab, search.statusProfile).get();
    };

    this.getTotalProfilesNodeAssociation = function(search,tab) {
        var params = {
            id: search.idCourtCase,
            pageSize: search.pageSize,
            page: search.page,
            input: search.searchText,
            isReference: tab,
            profiles: search.profilesCod,
            statusProfile: search.statusProfile
        };    
        return $http.get('/court-case-profiles-node-association', { params: params });
        //return playRoutes.controllers.Pedigrees.getTotalProfilesNodeAssociation(search.idCourtCase,search.pageSize,search.page,search.searchText, tab,search.profilesCod,search.statusProfile).get();
    };

    this.addProfiles = function(profiles) {
        console.log('ADD PROFILES');
        return $http.post('/court-case-profiles', profiles);
        //return playRoutes.controllers.Pedigrees.addProfiles().post(profiles);
    };

    this.removeProfiles = function(profiles) {
        return $http.put('/court-case-profiles', profiles);
        //return playRoutes.controllers.Pedigrees.removeProfiles().put(profiles);
    };

    this.filter = function(input,idCase,tab,tipo,pages,pageSizes) {
        return $http.get('/filter/profileDataForPedigree', {
            params: {
                input: input,
                idCase: idCase,
                tab: tab,
                tipo: tipo,
                page: pages,
                pageSize: pageSizes
            }
        });
        //return playRoutes.controllers.Pedigrees.filterProfilesForPedigree(input,idCase,tab,tipo,pages,pageSizes).get();
    };

    this.count = function(input,idCase,tab,tipo) {
        return $http.get('/filter/profileDataForPedigree/count', {
            params: {
                input: input,
                idCase: idCase,
                tab: tab,
                tipo: tipo
            }
        });
        //return playRoutes.controllers.Pedigrees.countProfilesForPedigree(input,idCase,tab,tipo).get();
    };

    this.getBatchSearchModalViewByIdOrLabel = function(input,idCase) {
        return $http.get('/batch-modal', {
            params: {
                input: input,
                idCase: idCase
            }
        });
        //return playRoutes.controllers.BulkUpload.getBatchSearchModalViewByIdOrLabel(input,idCase).get();
    };

    this.addBatches = function(req) {
        console.log('ADD BATCHES');
        return $http.post('/batch-modal-import', req);
        //return playRoutes.controllers.Pedigrees.addBatches().post(req);
    };

    this.getProfilesNodeAssociation = function(search, tab) {
        return $http.get('/court-case-profiles-node-association', {
            params: {
                id: search.idCourtCase,
                pageSize: search.pageSize,
                page: search.page,
                input: search.searchText,
                isReference: tab,
                profiles: search.profilesCod,
                statusProfile: search.statusProfile
            }
        });
        //return playRoutes.controllers.Pedigrees.getProfilesNodeAssociation(search.idCourtCase,search.pageSize,search.page,search.searchText,tab, search.profilesCod,search.statusProfile).get();
    };

    this.getTotalProfiles = function(search, tab) {
        return $http.get('/court-case-profiles/total', {
            params: {
                id: search.idCourtCase,
                pageSize: search.pageSize,
                page: search.page,
                input: search.searchText || null,
                isReference: tab,
                statusProfile: search.statusProfile || null
            }
        });
        //return playRoutes.controllers.Pedigrees.getTotalProfiles(search.idCourtCase,search.pageSize,search.page,search.searchText,tab,search.statusProfile).get();
    };

    this.getCourtCasePedigrees = function(search) {
        console.log('GET COURSE CASE PEDIGREES');
        return $http.post('/court-case-pedigrees', search);
        //return playRoutes.controllers.Pedigrees.getCourtCasePedigrees().post(search);
    };

    this.getTotalCourtCasePedigrees = function(search) {
        return $http.get('/court-case-pedigrees/total', {
            params: {
                id: search.idCourtCase,
                pageSize: search.pageSize,
                page: search.page,
                input: search.searchText || null,
                status: search.status || null
            }
        });
        //return playRoutes.controllers.Pedigrees.getTotalCourtCasePedigrees(search.idCourtCase,search.pageSize,search.page,search.searchText, search.status).get();
    };
	
	this.profileNodo=function (profile) {
        return $http.get('/pedigree/profiles', {
            params: {
                idCourtCase: profile.idCourtCase,
                codigoGlobal: profile.globalCode
            }
        });
        //return playRoutes.controllers.Pedigrees.profileNodo(profile.idCourtCase, profile.globalCode).get();
    };

    this.updateMetadata = function(id, personData){
        var user = userService.getUser();
        return $http.put('/pedigree/update-metadata', personData, {
            params: {
                idCourtCase: id,
                assignee: user.name
            }
        });
        //return playRoutes.controllers.Pedigrees.updateMetadata(id, user.name).put(personData);
    };

    this.createMetadata = function(id, personData){
        console.log('CREATE METADATA');
        return $http.post('/pedigree/metadata', personData, { params: { idCourtCase: id } });
        //return playRoutes.controllers.Pedigrees.createMetadata(id).post(personData);
    };

    this.removeMetadata= function(idCourtCase,personData) {
        return $http.put('/pedigree/remove-metadata', personData, { params: { idCourtCase: idCourtCase } });
        //return playRoutes.controllers.Pedigrees.removeMetadata(idCourtCase).put(personData);
    };

    this.getMetadata= function(buscar,idCourtCase){
        return $http.get('/pedigree/filtrar-alias', {
            params: {
                input: buscar.input,
                pageSize: buscar.pageSize,
                page: buscar.page,
                idCourtCase: idCourtCase
            }
        });
		//return playRoutes.controllers.Pedigrees.getMetadata(buscar.input,buscar.pageSize,buscar.page,idCourtCase).get();
	};

    this.getTotalMetadata = function (buscar) {
        return $http.get('/pedigree/total-alias', {
            params: {
                idCourtCase: buscar.idCourtCase,
                pageSize: buscar.pageSize,
                page: buscar.page,
                input: buscar.input
            }
        });
		//return playRoutes.controllers.Pedigrees.getTotalMetadata(buscar.idCourtCase,buscar.pageSize,buscar.page,buscar.input).get();
    };

    this.collapse = function(courtCaseId) {
        console.log('COLLAPSE');
        return $http.post('/collapsing', courtCaseId);
        //return playRoutes.controllers.Pedigrees.collapse().post(courtCaseId);
    };

    this.getPedCheck = function(pedigreeId,courtCaseId) {
        return $http.get('/pedcheck', { params: { pedigreeId: pedigreeId, idCourtCase: courtCaseId } });
        //return playRoutes.controllers.Pedigrees.getPedCheck(pedigreeId,courtCaseId).get();
    };

    this.generatePedCheck = function(pedigreeId,courtCaseId) {
        console.log('GENERATE PED CHECK');
        return $http.post('/pedcheck', { pedigreeId: pedigreeId, idCourtCase: courtCaseId });
        //return playRoutes.controllers.Pedigrees.generatePedCheck(pedigreeId,courtCaseId).post();
    };

    this.disassociateGroupedProfiles= function(profilesGrouped){
        return $http.put('/grouped-profiles/disassociate', profilesGrouped);
		//return playRoutes.controllers.Pedigrees.disassociateGroupedProfiles().put(profilesGrouped);
	};

    this.getTotalProfilesInactive = function(search) {
        return $http.get('/grouped-profiles/total-grouped', {
            params: {
                id: search.idCourtCase,
                pageSize: search.pageSize,
                page: search.page,
                input: search.searchText,
                statusProfile: search.statusProfile,
                groupedBy: search.groupedBy
            }
        });
        //return playRoutes.controllers.Pedigrees.getTotalProfilesInactive(search.idCourtCase,search.pageSize,search.page,search.searchText,search.statusProfile,search.groupedBy).get();
    };

    this.getProfilesInactive = function(search) {
        return axios.get('/grouped-profiles', {
            params: {
                id: search.idCourtCase,
                pageSize: search.pageSize,
                page: search.page,
                input: search.searchText || undefined,
                statusProfile: search.statusProfile,
                groupedBy: search.groupedBy
            } 
        });
        //return playRoutes.controllers.Pedigrees.getProfilesInactive(search.idCourtCase,search.pageSize,search.page,search.searchText,search.statusProfile, search.groupedBy).get();
    };
    
    this.areAssignedToPedigree =function (profilesGrouped) {
        console.log('ARE ASSIGNED TO PEDIGREE');
        return $http.post('/collapsing-manual', profilesGrouped);
        //return playRoutes.controllers.Pedigrees.areAssignedToPedigree().post(profilesGrouped);
    };

    this.doesntHaveActivePedigrees = function (idCourtCase) {
        return $http.get('/grouped-profiles/validation', {
            params: { idCourtCase: idCourtCase }
        });
		//return playRoutes.controllers.Pedigrees.doesntHaveActivePedigrees(idCourtCase).get();
    };

    this.getActiveMutationModels = function()  {
        return $http.get('/mutation-models/active');
        //return playRoutes.controllers.MutationController.getActiveMutationModels().get();
    };
};
return PedigreeService;

});