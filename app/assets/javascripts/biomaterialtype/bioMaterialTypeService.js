define([], function() {
'use strict';

	function BioMaterialTypeService(playRoutes,$http) {
		
		this.getBioMaterialTypes = function() {
			return playRoutes.controllers.BioMaterialTypes.list().get();
		};
		
		this.addBioMaterialType = function(bmt) {
			console.log('ADD BIO MATERIAL TYPES');
			return $http.post('/bioMaterialTypes', bmt);			
			//return playRoutes.controllers.BioMaterialTypes.insert().post(bmt);
		};
		
		this.updateBioMaterialType = function(bmt) {
			return playRoutes.controllers.BioMaterialTypes.update().put(bmt);
		};
		
		this.deleteBioMaterialType = function(id) {
			return playRoutes.controllers.BioMaterialTypes.remove(id).delete();
		};
	}

	return BioMaterialTypeService;
});