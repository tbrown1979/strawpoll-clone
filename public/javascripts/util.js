function getIdFromUrl() {
  var url = window.location.pathname;
  var id = url.substring(url.lastIndexOf('/') + 1);
  return id;
}
