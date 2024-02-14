define([ 'angular' ], function(angular) {
'use strict';

function ProfileService(playRoutes, $log, $q, userService) {

	function convertProfileToLabels(labeledGenotypification) {
		var ret = {}, i = 1;
		Object.keys(labeledGenotypification).forEach(function(l){ret[l] = {id: i++, caption: l};});
		return ret;
	}
	
	this.convertProfileToLabels = function(labeledGenotypification) {
		return convertProfileToLabels(labeledGenotypification);
	};
	
	function getLabels (labelsSets, profile) {
		var ret = {};
		
		if (angular.isObject(profile.labeledGenotypification) && Object.keys(profile.labeledGenotypification).length > 0) {
			
			ret = convertProfileToLabels(profile.labeledGenotypification);
			
			var usedLabel = Object.keys(profile.labeledGenotypification)[0];
			angular.forEach(labelsSets, function(labels/*, setId*/) {
				if (Object.keys(labels).some(function(l){return l === usedLabel;})) {
					ret = labels;
				}
			});
		} else {
			ret =  labelsSets.set1;
		}
		return ret;
	}
	
	this.getProfile = function(globalCode) {
		var deferred = $q.defer();

		var profileOpt = $q.defer();
		
		playRoutes.controllers.Profiles.getFullProfile(globalCode).get().then(
			function(response){
				profileOpt.resolve(response);
			},
			function(){
				profileOpt.resolve({});
			}
		);
		
		$q.all({
			profileData: playRoutes.controllers.ProfileData.findByCodeWithAssociations(globalCode).get(),
			profileOpt: profileOpt.promise,
			labelsSets: playRoutes.controllers.Profiles.getLabelsSets().get()
		}).then(
			function(response){
				response.data = angular.extend(response.profileData.data, response.profileOpt.data);
				response.data.labelsSets = response.labelsSets.data;
				response.data.labels = getLabels(response.data.labelsSets, response.profileOpt.data);
				deferred.resolve(response);
			},
			function(response){
				deferred.reject(response);
			}
		);

		return deferred.promise;
	};

	/**
	 * returns all the profiles global codes.
	 * @returns All the profiles global codes.
	 */
	this.profilesAll = function() {
		return playRoutes.controllers.Profiles.profilesAll().get();
	};
	
	this.profilesCategories = function(categories) {
		return playRoutes.controllers.Profiles.profilesCategories(categories).get();
	};

	this.findByCode = function(globalCode) {
		return playRoutes.controllers.Profiles.findByCode(globalCode).get();
	};
	
	this.findByCodes = function(globalCodes) {
		return playRoutes.controllers.Profiles.findByCodes(globalCodes).get();
	};
	
	this.getStrKits = function() {
		// $log.info('calling service: StrKits.list()');
		return playRoutes.controllers.StrKits.list().get();
	};
	
	this.saveFirstAutosomal = function(newAnalysis) {

		newAnalysis.userId = userService.getUser().name;
		
		var genotypification = newAnalysis.genotypification;
			
		angular.forEach(genotypification, function(values, locus) {
			genotypification[locus] = values.filter(function(alelle) { 
				return !(alelle === null || (alelle.trim && alelle.trim() === "")); 
			});
		});
		
		return playRoutes.controllers.Profiles.create().post(newAnalysis);
	};
	
	this.saveUploaded = function(uploadToken) {
		return playRoutes.controllers.Profiles.storeUploadedAnalysis(uploadToken).post({});
	};
	
	this.getLociByStrKitId = function(idKit) {
		// $log.info('calling service: StrKits.findLociByKit: ' + idKit);
		return playRoutes.controllers.StrKits.findLociByKit(idKit).get();
	};
	
	this.getElectropherogramsByCode = function(globalCode) {
		$log.info('getElectropherograms: ' + globalCode);				
		return playRoutes.controllers.Profiles.getElectropherogramsByCode(globalCode).get();
	};
    this.getFilesByCode = function(globalCode) {
        $log.info('getFiles: ' + globalCode);
        return playRoutes.controllers.Profiles.getFilesByCode(globalCode).get();
    };
    this.getFilesByAnalysisId = function(globalCode,analysisId) {
        return playRoutes.controllers.Profiles.getFilesByAnalysisId(globalCode,analysisId).get();
    };
	this.getElectropherogramsByAnalysisId = function(globalCode,analysisId) {
		return playRoutes.controllers.Profiles.getElectropherogramsByAnalysisId(globalCode,analysisId).get();
	};
	this.saveLabels = function(labels) {
		return playRoutes.controllers.Profiles.saveLabels().post(labels);
	};
	
	this.findSubcategoryRelationships = function(subcatId) {
		return playRoutes.controllers.Profiles.findSubcategoryRelationships(subcatId).get();
	};

	this.getSubcategories = function() {
		return playRoutes.controllers.Categories.list().get();
	};
	
	this.addElectropherograms = function(token, globalCode, idAnalysis, name) {
		return playRoutes.controllers.Profiles.addElectropherograms(token, globalCode, idAnalysis, name).post();
	};

    this.addFiles = function(token, globalCode, idAnalysis,name) {
        return playRoutes.controllers.Profiles.addFiles(token, globalCode, idAnalysis,name).post();
    };
	
	this.getLabels = function(sampleCode) {
		return playRoutes.controllers.Profiles.getLabels(sampleCode).get();
	};
    this.uploadProfile = function(globalGlode) {
        return playRoutes.controllers.Interconnections.uploadProfile(globalGlode).post();
    };
    this.isReadOnly = function(sampleCode) {
        return playRoutes.controllers.Profiles.isReadOnly(sampleCode).get();
    };

	this.removeFile = function(fileId) {
		return playRoutes.controllers.Profiles.removeFile(fileId).delete();
	};

	this.removeEpg = function(fileId) {
		return playRoutes.controllers.Profiles.removeEpg(fileId).delete();
	};
}

return ProfileService;

});