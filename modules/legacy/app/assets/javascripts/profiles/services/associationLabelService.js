define([ 'angular' ], function(angular) {
'use strict';

function AssociationLabelService(playRoutes) {
	
	function lowStringencyExists (associations) {
		var ret = false;
		angular.forEach(associations, function(matchResult) {
			if (matchResult.stringency === 'LowStringency') { ret = true; }
		});
		return ret;
	}
	
	function getMergedLoci(evidence, associations) {
		var loci = Object.keys(evidence);
		angular.forEach(associations, function(gcMatch) {loci = loci.concat(Object.keys(gcMatch.genotypification));});
		
		var ret = {};
		loci.forEach(function(x){ret[x] = {};});
		
		return ret;
	}
	
	this.verifyMixtureAssociation = function(mixtureGenot, profile, subcatId) {
		var post = {
			mixtureGenotypification: mixtureGenot, 
			profile: profile,
			subcategoryId: subcatId
		};
		
		return playRoutes.controllers.Profiles.verifyMixtureAssociation().post(post)
		.success(function(data) {
			if (data.result) {
				if (lowStringencyExists(data.associations)) {
					data.mergedLoci = getMergedLoci(mixtureGenot, data.associations);
				} 
			}
		}).error(function() {
		});
	};
	
}

return AssociationLabelService;

});