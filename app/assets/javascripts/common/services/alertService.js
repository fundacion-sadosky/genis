define(['angular', 'jquery'], function (angular) {
  'use strict';

  var mod = angular.module('common.alertService', []);

  mod.service('alertService', ['$animate', '$document', function ($animate, $document) {
	
	var showInterval = 5 * 1000;
	
	function createAlertStr(type, options, order) {
		return '<div id="pdgAlertInfo' + order + '" ' +
		'class="alert alert-' + type +' alert-dismissible pdg-alert pdg-alert-hide" role="alert">' +
		'<button type="button" class="close" data-dismiss="alert" aria-label="Close">'+
		'<span aria-hidden="true">&times;</span></button>' + options.message + '</div>';
	}
	
	function show(type, options, autodismissible) {
		var parent = $document.find(((options.parent)? '#' + options.parent: 'body')); // $document.find('#page-top');

		var alerts = $document.find('.alert');
		var order = alerts.length;
		var top = order > 0 ? alerts.position().top: parent.position().top;
		var height = order > 0 ? alerts.innerHeight(): 0;
		
		var alertElement = createAlertStr(type, options, order);
		
		$animate.enter(alertElement, parent);
		
		var elem = $document.find('#pdgAlertInfo' + order);
		
		setTimeout(function(){
			elem.removeClass("pdg-alert-hide");
			elem.addClass("pdg-alert-show");
			elem.css("top", top + height + 5);
		}, 1);
		
		if (autodismissible) {
			setTimeout(function(){
				elem.removeClass("pdg-alert-show");
				elem.addClass("pdg-alert-hide");
			}, showInterval);
			
			setTimeout(function(){
				elem.remove();
			}, showInterval + 2 * 1000);
		}
	}
	
	this.success = function(options) {
		show('success', options, true);
	};
	
	this.info = function(options) {
		show('info', options, true);
	};
	
	this.warning = function(options) {
		show('warning', options, false);
	};
	
	this.error = function(options) {
		show('danger', options, false);
	};

  }]);

  return mod;
}); // define
