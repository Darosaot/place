/*******************************************************************************
 * Copyright 2021 Subdirecci�n General de Coordinaci�n de la Contrataci�n Electronica - Direcci�n General Del Patrimonio Del Estado - Subsecretar�a de Hacienda - Ministerio de Hacienda - Administraci�n General del Estado - Gobierno de Espa�a
 * 
 * Licencia con arreglo a la EUPL, Versi�n 1.2 o �en cuanto sean aprobadas por la Comisi�n Europea� versiones posteriores de la EUPL (la �Licencia�);
 * Solo podr� usarse esta obra si se respeta la Licencia.
 * Puede obtenerse una copia de la Licencia en:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Salvo cuando lo exija la legislaci�n aplicable o se acuerde por escrito, el programa distribuido con arreglo a la Licencia se distribuye �TAL CUAL�, SIN GARANT�AS NI CONDICIONES DE NING�N TIPO, ni expresas ni impl�citas.
 * V�ase la Licencia en el idioma concreto que rige los permisos y limitaciones que establece la Licencia.
 ******************************************************************************/
package es.age.dgpe.placsp.risp.parser.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.purl.atompub.tombstones._1.DeletedEntryType;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.FeedType;
import org.w3._2005.atom.LinkType;

import es.age.dgpe.placsp.risp.parser.MainApp;
import es.age.dgpe.placsp.risp.parser.model.DatosCPM;
import es.age.dgpe.placsp.risp.parser.model.DatosEMP;
import es.age.dgpe.placsp.risp.parser.model.DatosLicitacionGenerales;
import es.age.dgpe.placsp.risp.parser.model.DatosResultados;
import es.age.dgpe.placsp.risp.parser.model.SpreeadSheetManager;
import ext.place.codice.common.caclib.ContractFolderStatusType;
import ext.place.codice.common.caclib.PreliminaryMarketConsultationStatusType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ParserController implements Initializable {

	private final Logger logger = LogManager.getLogger(ParserController.class.getName());

	@FXML
	private CheckBox checkLink;

	@FXML
	private CheckBox checkNumExpediente;

	@FXML
	private CheckBox checkObjetoContrato;

	@FXML
	private TextField textFieldDirOrigen;

	@FXML
	private TextField textFieldOutputFile;

	@FXML
	private RadioButton rbUnaTabla;

	@FXML
	private RadioButton rbDosTablas;


	@FXML
	private TreeView<String> treeDatos;

	@FXML
	private CheckBoxTreeItem<String> rootItem;

	@FXML
	private Text n_licitaciones;

	@FXML
	private Text n_ficheros;


	@FXML
	private ProgressBar progreso;

	@FXML
	private Button buttonGenerar;
	
	@FXML
	private Button buttonDirIn;
	
	@FXML
	private Button buttonDirOut;

	// Reference to the main application.
	private MainApp mainApp;

	private static Unmarshaller atomUnMarshaller;

	Alert aError = new Alert(AlertType.NONE);  


	ArrayList<DatosLicitacionGenerales> seleccionLicitacionGenerales;
	ArrayList<DatosResultados> seleccionLicitacionResultados;
	ArrayList<DatosEMP> seleccionEncargosMediosPropios;
	ArrayList<DatosCPM> seleccionConsultasPreliminares;


	/*
	 * initialize se ejecuta despu�s del constructor y sirve para la carga de datos din�mica, en este caso, del TreeView con las variables de los datos
	 */
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.rootItem = new CheckBoxTreeItem<String> ("Publicaciones");
		rootItem.setExpanded(true);
		this.treeDatos.setEditable(true);
		this.treeDatos.setCellFactory(CheckBoxTreeCell.<String>forTreeView());


		//Se crea el nodo para los datos de las licitaciones. Tendr� dos subnodos (generales y resultados)
		CheckBoxTreeItem<String> datosLicitaciones = new CheckBoxTreeItem<String> ("Datos de la licitaci�n");
		rootItem.getChildren().add(datosLicitaciones);

		CheckBoxTreeItem<String> datosLicitacioneGenerales = new CheckBoxTreeItem<String> ("Datos Generales");
		datosLicitaciones.getChildren().add(datosLicitacioneGenerales);
		for (DatosLicitacionGenerales dato: DatosLicitacionGenerales.values()) {
			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String> (dato.getTiulo());
			datosLicitacioneGenerales.getChildren().add(item);
		}

		CheckBoxTreeItem<String> datosResultados = new CheckBoxTreeItem<String> ("Datos de Resultados");
		datosLicitaciones.getChildren().add(datosResultados);
		for (DatosResultados dato: DatosResultados.values()) {
			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String> (dato.getTiulo());     
			datosResultados.getChildren().add(item);
		}


		//Se crea el nodo para los encargos a medios propios
		CheckBoxTreeItem<String> datosEMP = new CheckBoxTreeItem<String> ("Encargos a medios propios");
		rootItem.getChildren().add(datosEMP);
		for (DatosEMP dato: DatosEMP.values()) {
			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String> (dato.getTiulo());     
			datosEMP.getChildren().add(item);
		}



		//Se crea el nodo para las consultas preliminares de mercado
		CheckBoxTreeItem<String> datosCPM = new CheckBoxTreeItem<String> ("Consultas preliminares de mercado");
		rootItem.getChildren().add(datosCPM);
		for (DatosCPM dato: DatosCPM.values()) {
			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<String> (dato.getTiulo());     
			datosCPM.getChildren().add(item);
		}



		this.treeDatos.setRoot(rootItem);
		this.treeDatos.setShowRoot(true);

		//no deber�a hacer falta apriori, pero as� forzamos y nos aseguramos de que salgan las variables como opciones
		this.treeDatos.refresh();

		//Paso de la funci�n de selecci�n de rutas de directorios para datos de entrada y de salida
		buttonDirIn.setOnAction(e -> selectDir(textFieldDirOrigen, true));
		buttonDirOut.setOnAction(e -> selectDir(textFieldOutputFile, false));
	}


	/**
	 * The constructor. The constructor is called before the initialize() method.
	 */
	public ParserController() {

	}

	/**
	 * Is called by the main application to give a reference back to itself.
	 * 
	 * @param mainApp
	 */
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
	}
	
	private void selectDir(TextField inout, boolean isIn) {
		String previousPath = inout.getText().trim();
		FileChooser fileChooser = new FileChooser();
		if(isIn) {
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("ATOM", "*.atom"));
		} else {
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("XLSX", "*.xlsx"));
		}
		if(previousPath.length()>0){
			Path currentPath = Paths.get(previousPath);
			fileChooser.setInitialDirectory(new File(currentPath.getParent().toString()));
		}
		Stage currentStage = (Stage) inout.getScene().getWindow();
		//la diferencia principal con el selectDirIn es el showSaveDialog en lugar de showOpenDialog y la restricci�n de extensi�n
		File file;
		if(isIn) {
			file = fileChooser.showOpenDialog(currentStage);
		} else {
			file = fileChooser.showSaveDialog(currentStage);
		}
		
		if (file != null) {
			inout.setText(file.getAbsolutePath());
		}
	}

	public Task<Boolean> procesarDirectorio() {
		return new Task<Boolean>() {
			@SuppressWarnings("finally")
			@Override
			protected Boolean call() throws Exception {

				// Collecci�n que registra las entries que ya han sido procesadas
				HashSet<String> entriesProcesadas = new HashSet<String>();
				HashMap<String, GregorianCalendar> entriesDeleted = new HashMap<String, GregorianCalendar>();
				int numeroEntries = 0;
				int numeroFicherosProcesados = 0;

				FeedType res = null;
				FileOutputStream output_file = null;
				InputStreamReader inStream = null;

				seleccionLicitacionGenerales = new ArrayList<DatosLicitacionGenerales>();
				seleccionLicitacionResultados = new ArrayList<DatosResultados>();
				seleccionEncargosMediosPropios = new ArrayList<DatosEMP>();
				seleccionConsultasPreliminares = new ArrayList<DatosCPM>();


				try {

					updateProgress(0, 1);

					//Se crea el Stream de salida en el path indicado
					output_file = new FileOutputStream(new File(textFieldOutputFile.getText()));

					logger.debug("Se realiza la revisi�n de los datos seleccionados");
					recogerDatosSeleccionados();

					// Create the JAXBContext
					JAXBContext jc = JAXBContext.newInstance(
							"org.w3._2005.atom:org.dgpe.codice.common.caclib:org.dgpe.codice.common.cbclib:ext.place.codice.common.caclib:ext.place.codice.common.cbclib:org.purl.atompub.tombstones._1");
					atomUnMarshaller = jc.createUnmarshaller();


					//Se crean las hojas necesarias
					logger.debug("Creaci�n de hojas de c�lculo");
					SpreeadSheetManager spreeadSheetManager = new SpreeadSheetManager(rbDosTablas.isSelected(), seleccionEncargosMediosPropios.size()>0, seleccionConsultasPreliminares.size()>0);

					logger.debug("Se comienzan a a�adir los t�tulos");
					insertarTitulos(spreeadSheetManager);
					//Se cambian los tama�os de las columnas
					spreeadSheetManager.updateColumnsSize();
					logger.info("T�tulos a�adidos y tama�os de columnas ajustados");

					// Se comprueba que exista el ficheroRISP a procesar
					File ficheroRISP = new File(textFieldDirOrigen.getText());
					String directorioPath = ficheroRISP.getParent();
					boolean existeFicheroRisp = ficheroRISP.exists() && ficheroRISP.isFile();

					if (existeFicheroRisp) {
						logger.info("Directorio originen de ficheros RISP-PLACSP: " + directorioPath);
						logger.info("Fichero r�iz: " + ficheroRISP.getName());
					} else {
						logger.error("No se puede acceder al fichero " + textFieldDirOrigen.getText());
					}

					File[] lista_ficherosRISP = ficheroRISP.getParentFile().listFiles();
					logger.info("N�mero previsto de ficheros a procesar: " + lista_ficherosRISP.length);

					// calculo de cada salto
					double saltoBar = 1.00 / lista_ficherosRISP.length;
					double saltoAcumuladoBar = 0;

					while (existeFicheroRisp) {
						logger.info("Procesando fichero: " + ficheroRISP.getName());

						saltoAcumuladoBar += saltoBar;
						updateProgress(saltoAcumuladoBar, 1);
						logger.info("Ratio de archivos procesados: " + saltoAcumuladoBar * 100.00 + " %");

						res = null;
						inStream = new InputStreamReader(new FileInputStream(ficheroRISP), StandardCharsets.UTF_8);
						res = ((JAXBElement<FeedType>) atomUnMarshaller.unmarshal(inStream)).getValue();

						// Se a�aden las licitaciones que han dejado de ser v�lidas
						if (res.getAny() != null) {
							for (int indice = 0; indice < res.getAny().size(); indice++) {
								DeletedEntryType deletedEntry = ((JAXBElement<DeletedEntryType>) res.getAny().get(indice)).getValue();
								if (!entriesDeleted.containsKey(deletedEntry.getRef())) {
									entriesDeleted.put(deletedEntry.getRef(), deletedEntry.getWhen().toGregorianCalendar());
								}
							}
						}

						// Se recorren las licitaciones (elementos entry)
						numeroEntries += res.getEntry().size();
						for (EntryType entry : res.getEntry()) {
							// Se comprueba si ya se ha procesado una entry con el mismo identoficador y que es m�s reciente
							if (!entriesProcesadas.contains(entry.getId().getValue())) {
								// Se comprueba si se encuentra en la la lista de licitaciones Deleted
								GregorianCalendar fechaDeleted = null;
								if (entriesDeleted.containsKey(entry.getId().getValue())) {
									fechaDeleted = entriesDeleted.get(entry.getId().getValue());
								}

								//Se compruebe si se trata de una licitaci�n, un encargo a medio propio o un una consulta preliminar de mercado
								boolean isCPM = false;
								if (((JAXBElement<?>)entry.getAny().get(0)).getValue() instanceof PreliminaryMarketConsultationStatusType) {
									isCPM = true;
								}
								
								if (isCPM) {
									//Se trata de una consulta preliminar de mercado, solo cuando se han seleccionado campos
									if(seleccionConsultasPreliminares.size()>0) {
										procesarCPM(entry, spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.CPM), fechaDeleted, seleccionConsultasPreliminares);
									}
								}else {
									//Se trata de una licitaci�n o de un encargo a medio propio
									//Si existe el resultCode con valor 11, entonces es EMP. Si no, es licitaci�n
									boolean isEMP = false;
									try {
										//Se comprueba si es un EMP
										isEMP = (((JAXBElement<ContractFolderStatusType>) entry.getAny().get(0)).getValue().getTenderResult().get(0).getResultCode().getValue().compareTo("11") == 0);
									}
									catch(Exception e){
										isEMP = false;
									}

									if (isEMP) {
										//Se trata de un EMP
										//y se han seleccionado campos
										if(seleccionEncargosMediosPropios.size()>0) {
											procesarEncargo(entry, spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.EMP), fechaDeleted, seleccionEncargosMediosPropios);	
										}
									} else {
										//Es una licitaci�n
										if (rbDosTablas.isSelected()) {
											//La salida es en dos tablas
											procesarEntry(entry, spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.LICITACIONES), fechaDeleted, seleccionLicitacionGenerales);
											procesarEntryResultados(entry, spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.RESULTADOS), fechaDeleted, seleccionLicitacionResultados);
										}else {
											//La salida es en una tabla
											procesarEntryCompleta(entry, spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.LICITACIONES), fechaDeleted, seleccionLicitacionGenerales, seleccionLicitacionResultados);
										}
									}

								}

								entriesProcesadas.add(entry.getId().getValue());
							}
						}
						// se comprueba cu�l es el siguiente fichero a procesar
						for (LinkType linkType : res.getLink()) {
							existeFicheroRisp = false;
							if (linkType.getRel().toLowerCase().compareTo("next") == 0) {
								String[] tempArray = linkType.getHref().split("/");
								String nombreSiguienteRIPS = tempArray[tempArray.length - 1];
								ficheroRISP = new File(directorioPath + "/" + nombreSiguienteRIPS);
								existeFicheroRisp = ficheroRISP.exists() && ficheroRISP.isFile();
							}
						}
						inStream.close();
						numeroFicherosProcesados++;
						updateProgress(1, 1);
					}

					logger.info("Creando el fichero " + textFieldOutputFile.getText());
					logger.info("N�mero de ficheros procesados " + numeroFicherosProcesados);
					logger.info("N�mero de elementos entry existentes: " + numeroEntries);
					logger.info("Licitaciones insertadas en el fichero: " + entriesProcesadas.size());

					spreeadSheetManager.insertarFiltro(seleccionLicitacionGenerales.size(), seleccionLicitacionResultados.size(), seleccionEncargosMediosPropios.size(), seleccionConsultasPreliminares.size());

					
					
					logger.info("Comienzo de escritura del fichero de salida");
					spreeadSheetManager.getWorkbook().write(output_file); // write excel document to output stream
					output_file.close(); // close the file
					spreeadSheetManager.getWorkbook().close();
					// para mostrar algunos resultados en la interfaz de usuario
					n_licitaciones.setText(Integer.toString(entriesProcesadas.size()));
					n_ficheros.setText(Integer.toString(numeroFicherosProcesados));

					
					
					logger.info("Fin del proceso de generaci�n del fichero");

					Platform.runLater(() -> {
						aError.setAlertType(AlertType.INFORMATION);
						aError.setHeaderText(null);
						aError.setContentText("El proceso de generaci�n del fichero ha terminado con �xito");
						aError.show();
					});

				} catch (JAXBException e) {// ventanas de error para las excepciones contempladas
					String auxError = "Error al procesar el fichero ATOM. No se puede continuar con el proceso.";
					logger.error(auxError);
					logger.debug(e.getStackTrace());
					Platform.runLater(() -> {
						aError.setAlertType(AlertType.ERROR);
						aError.setHeaderText(null);
						aError.setContentText(auxError);
						aError.show();
					});
				} catch (FileNotFoundException e) {
					String auxError = "Error al generar el fichero de salida. No se pudo crear un fichero en la ruta indicada o no pudo ser abierto por alguna otra raz�n.";
					logger.error(auxError);
					logger.debug(e.toString());
					Platform.runLater(() -> {
						aError.setAlertType(AlertType.ERROR);
						aError.setHeaderText(null);
						aError.setContentText(auxError);
						aError.show();
					});
				} catch (Exception e) {
					// error inesperado
					String auxError = "Error inesperado, revise la configuraci�n y el log...";
					e.printStackTrace();
					logger.error(auxError);
					logger.debug(e.getStackTrace());
					logger.debug(e.getMessage());
					Platform.runLater(() -> {
						aError.setAlertType(AlertType.ERROR);
						aError.setHeaderText(null);
						aError.setContentText(auxError);
						aError.show();
					});
				} finally {
					return true;
				}
			}


		};
	}






	/**
	 * Funci�n paara procesr una entry y extraer todos sus datos.
	 * @param entry
	 * @param sheet 
	 */
	private void procesarEntry(EntryType entry, SXSSFSheet sheet, GregorianCalendar fechaDeleted, ArrayList<DatosLicitacionGenerales> buscadorDatosSeleecionables) {		
		Cell cell;
		ContractFolderStatusType contractFolder = ((JAXBElement<ContractFolderStatusType>) entry.getAny().get(0)).getValue();

		Row row = sheet.createRow(sheet.getLastRowNum()+1);

		//Se obtienen los datos de la licitaci�n
		int cellnum = 0;

		//Se a�aden los datos b�sicos y obligatorios de la entry: id, lin y updated
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getId().getValue().substring(entry.getId().getValue().lastIndexOf("/")+1));
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getLink().get(0).getHref());



		//Se obtiene la fechaUpdated de la entry
		GregorianCalendar updated = entry.getUpdated().getValue().toGregorianCalendar();

		//La fecha de actualizaci�n ser� la m�s recinte comparando la �ltima entry con la fecha deleted si existe.
		if (fechaDeleted == null || fechaDeleted.compareTo(updated) < 0) {
			//La entry es v�lida, no hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)entry.getUpdated().getValue().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			cell.setCellValue("VIGENTE");
		}else {
			//La entry no es v�lida, hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)fechaDeleted.toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			if (((fechaDeleted.getTimeInMillis() - updated.getTimeInMillis())/1000/3660/24/365) > 5){
				cell.setCellValue("ARCHIVADA");
			}else {
				cell.setCellValue("ANULADA");
			}
		}

		for (DatosLicitacionGenerales dato: buscadorDatosSeleecionables) {
			Object datoCodice = dato.valorCodice(contractFolder); 
			cell = row.createCell(cellnum++);
			if (datoCodice instanceof BigDecimal) {
				cell.setCellValue((double) ((BigDecimal)datoCodice).doubleValue());
			}else if (datoCodice instanceof String) { 
				cell.setCellValue((String) datoCodice);
			}else if (datoCodice instanceof GregorianCalendar) {
				cell.setCellValue((LocalDateTime) ((GregorianCalendar)datoCodice).toZonedDateTime().toLocalDateTime());
			}else if (datoCodice instanceof Boolean) {
				cell.setCellValue((Boolean) datoCodice);
			}
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFormato(dato.getFormato()));

		}


	}



	private void procesarEntryResultados(EntryType entry, SXSSFSheet sheet, GregorianCalendar fechaDeleted, ArrayList<DatosResultados> buscadorDatosResultados) {	
		Cell cell;

		ContractFolderStatusType contractFolder = ((JAXBElement<ContractFolderStatusType>) entry.getAny().get(0)).getValue();

		//Se obtiene el n�mero de elementos tenderResult, en caso de que tenga,
		if(contractFolder.getTenderResult() != null) {			
			for (int indice = 0; indice < contractFolder.getTenderResult().size(); indice++) {
				Row row = sheet.createRow(sheet.getLastRowNum()+1);
				int cellnum = 0;

				//Se a�aden los datos b�sicos y obligatorios de la entry: id, link y vigencia
				cell = row.createCell(cellnum++);
				cell.setCellValue(entry.getId().getValue().substring(entry.getId().getValue().lastIndexOf("/")+1));
				cell = row.createCell(cellnum++);
				cell.setCellValue(entry.getLink().get(0).getHref());

				//Se obtiene la fechaUpdated de la entry
				GregorianCalendar updated = entry.getUpdated().getValue().toGregorianCalendar();

				//La fecha de actualizaci�n ser� la m�s recinte comparando la �ltima entry con la fecha deleted si existe.
				if (fechaDeleted == null || fechaDeleted.compareTo(updated) < 0) {
					//La entry es v�lida, no hay un deleted-entry posterior
					cell = row.createCell(cellnum++);
					cell.setCellValue((LocalDateTime)entry.getUpdated().getValue().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
					cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
				}else {
					//La entry no es v�lida, hay un deleted-entry posterior
					cell = row.createCell(cellnum++);
					cell.setCellValue((LocalDateTime)fechaDeleted.toZonedDateTime().toLocalDateTime());
					cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
				}

				for (DatosResultados dato: buscadorDatosResultados) {
					Object datoCodice = dato.valorCodice(contractFolder, indice); 
					cell = row.createCell(cellnum++);
					if (datoCodice instanceof BigDecimal) {
						cell.setCellValue((double) ((BigDecimal)datoCodice).doubleValue());
					}else if (datoCodice instanceof String) {
						cell.setCellValue((String) datoCodice);
					}else if (datoCodice instanceof GregorianCalendar) {
						cell.setCellValue((LocalDateTime) ((GregorianCalendar)datoCodice).toZonedDateTime().toLocalDateTime());
					}else if (datoCodice instanceof Boolean) {
						cell.setCellValue((Boolean) datoCodice);
					}
					cell.setCellStyle(SpreeadSheetManager.getCellStyleFormato(dato.getFormato()));
				}

			}
		}
	}

	/**
	 *  Se a�aden los datos en una �nica hoja. Los "datos seleccionables" se repiten por cada resultado. Si no hay resultados, se insertan una �nica vez
	 * @param entry
	 * @param sheet
	 * @param fechaDeleted
	 * @param buscadorDatosSeleecionables
	 * @param buscadorDatosResultados
	 */
	private void procesarEntryCompleta(EntryType entry, SXSSFSheet sheet, GregorianCalendar fechaDeleted,
			ArrayList<DatosLicitacionGenerales> buscadorDatosSeleccionables,
			ArrayList<DatosResultados> buscadorDatosResultados) {

		Cell cell;
		ContractFolderStatusType contractFolder = ((JAXBElement<ContractFolderStatusType>) entry.getAny().get(0)).getValue();

		//Se obtiene el n�mero de elementos tenderResult, en caso de que tenga,
		if(contractFolder.getTenderResult().size() > 0) {
			//hay resultados en esta entry, por lo que se insertan tantas filas como resultados

			for (int indice = 0; indice < contractFolder.getTenderResult().size(); indice++) {
				//Se insertan los datos comunes
				procesarEntry(entry, sheet, fechaDeleted, buscadorDatosSeleccionables);

				//En la misma fila, se completan con los datos del tenderresult
				Row row = sheet.getRow(sheet.getLastRowNum());
				int cellnum = buscadorDatosSeleccionables.size()+4;

				for (DatosResultados dato: buscadorDatosResultados) {
					Object datoCodice = dato.valorCodice(contractFolder, indice); 
					cell = row.createCell(cellnum++);
					if (datoCodice instanceof BigDecimal) {
						cell.setCellValue((double) ((BigDecimal)datoCodice).doubleValue());
					}else if (datoCodice instanceof String) {
						cell.setCellValue((String) datoCodice);
					}else if (datoCodice instanceof GregorianCalendar) {
						cell.setCellValue((LocalDateTime) ((GregorianCalendar)datoCodice).toZonedDateTime().toLocalDateTime());
					}else if (datoCodice instanceof Boolean) {
						cell.setCellValue((Boolean) datoCodice);
					}
					cell.setCellStyle(SpreeadSheetManager.getCellStyleFormato(dato.getFormato()));
				}		
			}	
		}else {
			//No hay resultados en esta entry, solo se inserta una �nica vez
			procesarEntry(entry, sheet, fechaDeleted, buscadorDatosSeleccionables);						
		}
	}


	/**
	 * M�todo que recorre el TreeView y vuelca en los arrayList los datos seleccionados
	 */
	private void recogerDatosSeleccionados() {
		HashMap<String, String> seleccionadosArbol;
		CheckBoxTreeItem<String> nodo ;

		//Datos Generales de la licitaci�n. Se recorre los datos posibles buscando los selecionados	
		seleccionadosArbol = new HashMap<String, String>();
		nodo = (CheckBoxTreeItem<String>) rootItem.getChildren().get(0).getChildren().get(0);
		seleccionadosArbol = findCheckedBoxes(nodo);

		for (DatosLicitacionGenerales datoLicitacionGeneral :  DatosLicitacionGenerales.values()) {
			if (seleccionadosArbol.containsKey(datoLicitacionGeneral.getTiulo())) {
				seleccionLicitacionGenerales.add(datoLicitacionGeneral);
			}
		}

		//Resultados de la licitaci�n. Se recorre los datos posibles buscando los selecionados
		seleccionadosArbol = new HashMap<String, String>();
		nodo = (CheckBoxTreeItem<String>) rootItem.getChildren().get(0).getChildren().get(1);
		seleccionadosArbol = findCheckedBoxes(nodo);
		for (DatosResultados datosResultados :  DatosResultados.values()) {
			if (seleccionadosArbol.containsKey(datosResultados.getTiulo())) {
				seleccionLicitacionResultados.add(datosResultados);
			}
		}


		//Encargos a medios propios. Se recorre los datos posibles buscando los selecionados	
		seleccionadosArbol = new HashMap<String, String>();
		nodo = (CheckBoxTreeItem<String>) rootItem.getChildren().get(1);
		seleccionadosArbol = findCheckedBoxes(nodo);
		for (DatosEMP datoEMP :  DatosEMP.values()) {
			if (seleccionadosArbol.containsKey(datoEMP.getTiulo())) {
				seleccionEncargosMediosPropios.add(datoEMP);
			}
		}


		//Consultas preliminares. Se recorre los datos posibles buscando los selecionados
		seleccionadosArbol = new HashMap<String, String>();
		nodo = (CheckBoxTreeItem<String>) rootItem.getChildren().get(2);
		seleccionadosArbol = findCheckedBoxes(nodo);
		for (DatosCPM datoCPM :  DatosCPM.values()) {
			if (seleccionadosArbol.containsKey(datoCPM.getTiulo())) {
				seleccionConsultasPreliminares.add(datoCPM);
			}
		}

		if (logger.isDebugEnabled()) {
			//Se imprimen los datos seleccionados
			for (DatosLicitacionGenerales  seleccion: seleccionLicitacionGenerales) {
				logger.debug(seleccion.getTiulo());
			}

			for (DatosResultados  seleccion: seleccionLicitacionResultados) {
				logger.debug(seleccion.getTiulo());
			}

			for (DatosEMP  seleccion: seleccionEncargosMediosPropios) {
				logger.debug(seleccion.getTiulo());
			}

			for (DatosCPM  seleccion: seleccionConsultasPreliminares) {
				logger.debug(seleccion.getTiulo());
			}

		}

	}



	private void procesarEncargo(EntryType entry, SXSSFSheet sheet, GregorianCalendar fechaDeleted, ArrayList<DatosEMP> buscadorDatosSelecionables) {		
		

		Cell cell;
		ContractFolderStatusType contractFolder = ((JAXBElement<ContractFolderStatusType>) entry.getAny().get(0)).getValue();

		Row row = sheet.createRow(sheet.getLastRowNum()+1);

		//Se obtienen los datos de la licitaci�n
		int cellnum = 0;

		//Se a�aden los datos b�sicos y obligatorios de la entry: id, lin y updated
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getId().getValue().substring(entry.getId().getValue().lastIndexOf("/")+1));
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getLink().get(0).getHref());



		//Se obtiene la fechaUpdated de la entry
		GregorianCalendar updated = entry.getUpdated().getValue().toGregorianCalendar();

		//La fecha de actualizaci�n ser� la m�s recinte comparando la �ltima entry con la fecha deleted si existe.
		if (fechaDeleted == null || fechaDeleted.compareTo(updated) < 0) {
			//La entry es v�lida, no hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)entry.getUpdated().getValue().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			cell.setCellValue("VIGENTE");
		}else {
			//La entry no es v�lida, hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)fechaDeleted.toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			if (((fechaDeleted.getTimeInMillis() - updated.getTimeInMillis())/1000/3660/24/365) > 5){
				cell.setCellValue("ARCHIVADA");
			}else {
				cell.setCellValue("ANULADA");
			}
		}

		for (DatosEMP dato: buscadorDatosSelecionables) {
			Object datoCodice = dato.valorCodice(contractFolder); 
			cell = row.createCell(cellnum++);
			if (datoCodice instanceof BigDecimal) {
				cell.setCellValue((double) ((BigDecimal)datoCodice).doubleValue());
			}else if (datoCodice instanceof String) { 
				cell.setCellValue((String) datoCodice);
			}else if (datoCodice instanceof GregorianCalendar) {
				cell.setCellValue((LocalDateTime) ((GregorianCalendar)datoCodice).toZonedDateTime().toLocalDateTime());
			}else if (datoCodice instanceof Boolean) {
				cell.setCellValue((Boolean) datoCodice);
			}
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFormato(dato.getFormato()));

		}

	}

	private void procesarCPM(EntryType entry, SXSSFSheet sheet, GregorianCalendar fechaDeleted, ArrayList<DatosCPM> buscadorDatosSelecionables) {		
		Cell cell;
		PreliminaryMarketConsultationStatusType preliminaryMarketConsultationStatusType = ((JAXBElement<PreliminaryMarketConsultationStatusType>) entry.getAny().get(0)).getValue();

		Row row = sheet.createRow(sheet.getLastRowNum()+1);

		//Se obtienen los datos de la licitaci�n
		int cellnum = 0;

		//Se a�aden los datos b�sicos y obligatorios de la entry: id, lin y updated
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getId().getValue().substring(entry.getId().getValue().lastIndexOf("/")+1));
		cell = row.createCell(cellnum++);
		cell.setCellValue(entry.getLink().get(0).getHref());



		//Se obtiene la fechaUpdated de la entry
		GregorianCalendar updated = entry.getUpdated().getValue().toGregorianCalendar();

		//La fecha de actualizaci�n ser� la m�s recinte comparando la �ltima entry con la fecha deleted si existe.
		if (fechaDeleted == null || fechaDeleted.compareTo(updated) < 0) {
			//La entry es v�lida, no hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)entry.getUpdated().getValue().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			cell.setCellValue("VIGENTE");
		}else {
			//La entry no es v�lida, hay un deleted-entry posterior
			cell = row.createCell(cellnum++);
			cell.setCellValue((LocalDateTime)fechaDeleted.toZonedDateTime().toLocalDateTime());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFechaLarga());
			cell = row.createCell(cellnum++);
			if (((fechaDeleted.getTimeInMillis() - updated.getTimeInMillis())/1000/3660/24/365) > 5){
				cell.setCellValue("ARCHIVADA");
			}else {
				cell.setCellValue("ANULADA");
			}
		}

		for (DatosCPM dato: buscadorDatosSelecionables) {
			Object datoCodice = dato.valorCodice(preliminaryMarketConsultationStatusType); 
			cell = row.createCell(cellnum++);
			if (datoCodice instanceof BigDecimal) {
				cell.setCellValue((double) ((BigDecimal)datoCodice).doubleValue());
			}else if (datoCodice instanceof String) { 
				cell.setCellValue((String) datoCodice);
			}else if (datoCodice instanceof GregorianCalendar) {
				cell.setCellValue((LocalDateTime) ((GregorianCalendar)datoCodice).toZonedDateTime().toLocalDateTime());
			}else if (datoCodice instanceof Boolean) {
				cell.setCellValue((Boolean) datoCodice);
			}
			cell.setCellStyle(SpreeadSheetManager.getCellStyleFormato(dato.getFormato()));

		}


	}



	//funcion auxiliar para recoger los checkboxes seleccionados
	private HashMap<String, String> findCheckedBoxes(CheckBoxTreeItem<String> nodoTree) {
		HashMap<String, String> checkedItems = new HashMap<String, String>();

		for (TreeItem<String> child : nodoTree.getChildren()) {
			if (((CheckBoxTreeItem<String>)child).isSelected()) {
				checkedItems.put(child.getValue(),  child.getValue());
			}			
		}
		return checkedItems;
	}


	/**
	 * M�todo que inserta los t�tulos en las hojas disponibles
	 * @param spreeadSheetManager
	 */
	private void insertarTitulos(SpreeadSheetManager spreeadSheetManager) {
		//Se a�aden los t�tulos de los elementos que se van a insertar en las hojas
		SXSSFSheet hoja;
		Row row;
		int cellnum;
		Cell cell;

		//HOJA LICITACIONES
		hoja = spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.LICITACIONES);
		row = hoja.createRow(0);
		cellnum = 0;
		cell = row.createCell(cellnum++);
		cell.setCellValue("Identificador");
		cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
		cell = row.createCell(cellnum++);
		cell.setCellValue("Link licitaci�n");
		cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
		cell = row.createCell(cellnum++);
		cell.setCellValue("Fecha actualizaci�n");
		cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
		cell = row.createCell(cellnum++);
		cell.setCellValue("Vigente/Anulada/Archivada");
		cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
		for (DatosLicitacionGenerales dato : seleccionLicitacionGenerales) {
			cell = row.createCell(cellnum++);
			cell.setCellValue((String) dato.getTiulo());
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
		}
		if (spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.RESULTADOS) == null) {
			for (DatosResultados dato : seleccionLicitacionResultados) {
				cell = row.createCell(cellnum++);
				cell.setCellValue((String) dato.getTiulo());
				cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			}
		}


		//HOJA Resultados
		if (spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.RESULTADOS) != null) {
			hoja = spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.RESULTADOS);
			row = hoja.createRow(0);
			cellnum = 0;
			cell = row.createCell(cellnum++);
			cell.setCellValue("Identificador");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Link licitaci�n");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Fecha actualizaci�n");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			for (DatosResultados dato : seleccionLicitacionResultados) {
				cell = row.createCell(cellnum++);
				cell.setCellValue((String) dato.getTiulo());
				cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			}
		}


		//HOJA Encargos a medios propios
		if (spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.EMP) != null) {
			hoja = spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.EMP);
			row = hoja.createRow(0);
			cellnum = 0;
			cell = row.createCell(cellnum++);
			cell.setCellValue("Identificador");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Link Encargo");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Fecha actualizaci�n");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Vigente/Anulada/Archivada");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			for (DatosEMP dato : seleccionEncargosMediosPropios) {
				cell = row.createCell(cellnum++);
				cell.setCellValue((String) dato.getTiulo());
				cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			}
		}

		//HOJA Consultas preliminares mercado
		if (spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.CPM) != null) {
			hoja = spreeadSheetManager.getWorkbook().getSheet(SpreeadSheetManager.CPM);
			row = hoja.createRow(0);
			cellnum = 0;
			cell = row.createCell(cellnum++);
			cell.setCellValue("Identificador");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Link Consulta");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Fecha actualizaci�n");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			cell = row.createCell(cellnum++);
			cell.setCellValue("Vigente/Anulada/Archivada");
			cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			for (DatosCPM dato : seleccionConsultasPreliminares) {
				cell = row.createCell(cellnum++);
				cell.setCellValue((String) dato.getTiulo());
				cell.setCellStyle(SpreeadSheetManager.getCellStyleTitulo());
			}
		}



	}



	@FXML
	private void generarXLSX(){
		progreso.progressProperty().unbind();
		progreso.setProgress(0);
		Task<Boolean> process = procesarDirectorio();
		progreso.progressProperty().unbind();
		progreso.progressProperty().bind(process.progressProperty());
		buttonGenerar.disableProperty().bind(process.runningProperty());
		new Thread(process).start();        
	}




}
