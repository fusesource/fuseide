/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.camel.model.service.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.TimePatternConverter;
import org.apache.camel.catalog.URISupport;
import org.apache.camel.catalog.karaf.KarafRuntimeProvider;
import org.apache.camel.catalog.maven.MavenVersionManager;
import org.apache.camel.catalog.springboot.SpringBootRuntimeProvider;
import org.apache.maven.model.Repository;
import org.fusesource.ide.camel.model.service.core.CamelSchemaProvider;
import org.fusesource.ide.camel.model.service.core.ICamelManagerService;
import org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates;
import org.fusesource.ide.camel.model.service.core.catalog.cache.CamelModel;
import org.fusesource.ide.camel.model.service.core.catalog.components.Component;
import org.fusesource.ide.camel.model.service.core.catalog.dataformats.DataFormat;
import org.fusesource.ide.camel.model.service.core.catalog.eips.Eip;
import org.fusesource.ide.camel.model.service.core.catalog.languages.Language;
import org.fusesource.ide.camel.model.service.core.util.CamelCatalogUtils;
import org.fusesource.ide.camel.model.service.core.util.CamelMavenUtils;

/**
 * @author lhein
 */
public class CamelService implements ICamelManagerService {
	
	private static final boolean ENCODE_DEFAULT = false;

	private Map<CamelCatalogCoordinates, CamelCatalog> cachedCatalogs = new HashMap<>();
	
	private MavenVersionManager tmpMan;
	private File tempFolder;

	public CamelService() {
		tmpMan = new MavenVersionManager();
		try {
			tempFolder = File.createTempFile("grape","m2repo");
			tempFolder.delete();
			tempFolder.mkdirs();
			tmpMan.setCacheDirectory(tempFolder.getPath());
		} catch (IOException ex) {
			CamelServiceImplementationActivator.pluginLog().logError(ex);
			tempFolder = null;
		} finally {
			for (List<String> rep : new CamelMavenUtils().getAdditionalRepos()) {
				tmpMan.addMavenRepository(rep.get(0), rep.get(1));
			}
		}
	}
	
	private CamelCatalog getCatalog(CamelCatalogCoordinates coords) {
		if (!cachedCatalogs.containsKey(coords) ) {
			CamelCatalog catalog = new DefaultCamelCatalog(true);
			MavenVersionManager versionManager = new MavenVersionManager();
			if (tempFolder != null) {
				versionManager.setCacheDirectory(tempFolder.getPath());
			}
			configureAdditionalRepos(versionManager);
			catalog.setVersionManager(versionManager);
			if (!catalog.loadVersion(coords.getVersion())) {
				CamelServiceImplementationActivator.pluginLog().logError("Unable to load Camel Catalog for version " + coords.getVersion());
			}
			if (!CamelCatalogUtils.isCamelVersionWithoutProviderSupport(coords.getVersion())) {
				configureRuntimeprovider(coords, catalog);
			}
			cachedCatalogs.put(coords, catalog);
		}
		return cachedCatalogs.get(coords);
	}

	private void configureRuntimeprovider(CamelCatalogCoordinates coords, CamelCatalog catalog) {
		String runtimeProvider = CamelCatalogUtils.getRuntimeProviderFromDependency(coords.asMavenDependency());
		if (CamelCatalogUtils.RUNTIME_PROVIDER_SPRINGBOOT.equalsIgnoreCase(runtimeProvider)) {
			catalog.setRuntimeProvider(new SpringBootRuntimeProvider());
//			} else if (CamelCatalogUtils.RUNTIME_PROVIDER_WILDFLY.equalsIgnoreCase(runtimeProvider)) {
//				catalog.setRuntimeProvider(new WildFlyRuntimeProvider());
		} else {
			catalog.setRuntimeProvider(new KarafRuntimeProvider());
		}
		if (!catalog.loadRuntimeProviderVersion(coords.getGroupId(), coords.getArtifactId(), coords.getVersion())) {
			CamelServiceImplementationActivator.pluginLog().logError(String.format("Unable to load the Camel Catalog for %s! Loaded %s as fallback.", coords, catalog.getCatalogVersion()));
		}
	}

	private void configureAdditionalRepos(MavenVersionManager versionManager) {
		List<List<String>> additionalM2Repos = new CamelMavenUtils().getAdditionalRepos();
		for (List<String> repo : additionalM2Repos) {
			String repoName = repo.get(0);
			String repoUri = repo.get(1);
			versionManager.addMavenRepository(repoName, repoUri);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#getCamelModel(java.lang.String)
	 */
	@Override
	public CamelModel getCamelModelForKarafRuntimeProvider(String camelVersion) {
		return this.getCamelModel(camelVersion, CamelCatalogUtils.RUNTIME_PROVIDER_KARAF);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#getCamelModel(java.lang.String, java.lang.String)
	 */
	@Override
	public CamelModel getCamelModel(String camelVersion, String runtimeProvider) {
		CamelCatalogCoordinates coords = CamelCatalogUtils.getCatalogCoordinatesFor(runtimeProvider, camelVersion);
		CamelCatalog catalog = getCatalog(coords);
		CamelModel loadedModel = loadCamelModelFromCatalog(catalog);
		CamelModelPatcher.applyVersionSpecificCatalogFixes(catalog, loadedModel);
		return loadedModel;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#getCamelSchemaProvider(org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public CamelSchemaProvider getCamelSchemaProvider(CamelCatalogCoordinates coords) {
		CamelCatalog catalog = getCatalog(coords);
		return new CamelSchemaProvider(catalog.blueprintSchemaAsXml(), catalog.springSchemaAsXml());
	}	
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#createEndpointUri(java.lang.String, java.util.Map, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public String createEndpointUri(String scheme, Map<String, String> properties, CamelCatalogCoordinates coords) throws URISyntaxException {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.asEndpointUri(scheme, properties, ENCODE_DEFAULT);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#createEndpointUri(java.lang.String, java.util.Map, boolean, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public String createEndpointUri(String scheme, Map<String, String> properties, boolean encode,
			CamelCatalogCoordinates coords) throws URISyntaxException {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.asEndpointUri(scheme, properties, encode);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#getEndpointProperties(java.lang.String, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public Map<String, String> getEndpointProperties(String uri, CamelCatalogCoordinates coords)
			throws URISyntaxException {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.endpointProperties(uri);
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#createEndpointXml(java.lang.String, java.util.Map, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public String createEndpointXml(String scheme, Map<String, String> properties, CamelCatalogCoordinates coords)
			throws URISyntaxException {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.asEndpointUriXml(scheme, properties, ENCODE_DEFAULT);
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#createEndpointXml(java.lang.String, java.util.Map, boolean, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public String createEndpointXml(String scheme, Map<String, String> properties, boolean encode,
			CamelCatalogCoordinates coords) throws URISyntaxException {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.asEndpointUriXml(scheme, properties, encode);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#getEndpointScheme(java.lang.String, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public String getEndpointScheme(String uri, CamelCatalogCoordinates coords) {
		CamelCatalog catalog = getCatalog(coords);
		return catalog.endpointComponentName(uri);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#testExpression(java.lang.String, java.lang.String)
	 */
	@Override
	public String testExpression(String language, String expression) {
		String result = null;
		org.apache.camel.impl.DefaultCamelContext ctx = new org.apache.camel.impl.DefaultCamelContext();
		try {
			ctx.resolveLanguage(language).createPredicate(expression.replaceAll("\n", "").replaceAll("\r", "").trim());
			result = null;
		} catch (Exception ex) {
			result = ex.getMessage();
		} finally {
			try {
				ctx.shutdown();
			} catch (Exception ex) {
				CamelServiceImplementationActivator.pluginLog().logError(ex);
			}
			ctx = null;
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#durationToMillis(java.lang.String)
	 */
	@Override
	public long durationToMillis(String duration) {
		return TimePatternConverter.toMilliSeconds(duration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#parseQuery(java.lang.String)
	 */
	@Override
	public Map<String, Object> parseQuery(String uri) throws URISyntaxException {
		return URISupport.parseQuery(uri);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#createQuery(java.util.Map)
	 */
	@Override
	public String createQuery(Map<String, Object> parameters) throws URISyntaxException {
		Map<String, String> params = new HashMap<>();
		for (Entry<String, Object> e : parameters.entrySet()) {
			params.put(e.getKey(), (String)e.getValue());
		}
		return URISupport.createQueryString(params, Character.toString('&'), ENCODE_DEFAULT);
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#updateMavenRepositoryLookup(java.util.List, org.fusesource.ide.camel.model.service.core.catalog.cache.CamelCatalogCoordinates)
	 */
	@Override
	public void updateMavenRepositoryLookup(List<Repository> repositories, CamelCatalogCoordinates coords) {
		for (Repository repo : repositories) {
			((MavenVersionManager)getCatalog(coords).getVersionManager()).addMavenRepository(repo.getId(), repo.getUrl());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.service.core.ICamelManagerService#isCamelVersionExisting(java.lang.String)
	 */
	@Override
	public boolean isCamelVersionExisting(String camelVersion) {
		return tmpMan.loadVersion(camelVersion);
	}
	
	private CamelModel loadCamelModelFromCatalog(CamelCatalog catalog) {
		CamelModel model = new CamelModel();
		
		for (String name : catalog.findComponentNames()) {
			String json = catalog.componentJSonSchema(name);
			Component elem = Component.getJSONFactoryInstance(new ByteArrayInputStream(getUnicodeEncodedStreamIfPossible(json)));
			model.addComponent(elem);
		}

		for (String name : catalog.findDataFormatNames()) {
			String json = catalog.dataFormatJSonSchema(name);
			DataFormat elem = DataFormat.getJSONFactoryInstance(new ByteArrayInputStream(getUnicodeEncodedStreamIfPossible(json)));
			model.addDataFormat(elem);
		}

		for (String name : catalog.findLanguageNames()) {
			String json = catalog.languageJSonSchema(name);
			Language elem = Language.getJSONFactoryInstance(new ByteArrayInputStream(getUnicodeEncodedStreamIfPossible(json)));
			model.addLanguage(elem);
		}

		for (String name : catalog.findModelNames()) {
			String json = catalog.modelJSonSchema(name);
			Eip elem = Eip.getJSONFactoryInstance(new ByteArrayInputStream(getUnicodeEncodedStreamIfPossible(json)));
			model.addEip(elem);
		}
		
		return model;
	}
	
	private byte[] getUnicodeEncodedStreamIfPossible(String json) {
		return json.getBytes(StandardCharsets.UTF_8);
	}
}

