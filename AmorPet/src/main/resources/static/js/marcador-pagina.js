
/* pega parte da url */
let urlPath = window.location.pathname;
marcadorPagina(urlPath);

function marcadorPagina(urlPath) {
	urlPath = urlPath.substr(1);
	$("#" + url).addClass('marcador-pagina').siblings('li').removeClass(
			'marcador-pagina');
}