define([], function() {
'use strict';

	function LaboratoriesService(playRoutes, $http) {

		this.getLaboratory = function(code){
			return playRoutes.controllers.Laboratories.getLaboratory(code).get();
		};

		this.getLaboratories = function(){
			return playRoutes.controllers.Laboratories.list().get();
		};
					
		this.getLaboratoriesDescriptive = function(){
			return playRoutes.controllers.Laboratories.listDescriptive().get();
		};
					
		this.getCountries = function(){
			return playRoutes.controllers.Laboratories.listCountries().get();
		};
		
		this.getProvinces = function(country){
			return playRoutes.controllers.Laboratories.listProvinces(country).get();
		};
		
		this.createLaboratory = function(laboratory){
			return  $http.post('/laboratory', laboratory);
			//return playRoutes.controllers.Laboratories.addLab().post(laboratory);
		};

		this.updateLaboratory = function(laboratory){
			return playRoutes.controllers.Laboratories.updateLab().put(laboratory);
		};
	}

	return LaboratoriesService;
});