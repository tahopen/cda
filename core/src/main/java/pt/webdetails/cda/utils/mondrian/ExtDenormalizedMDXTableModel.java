/*!
 * Copyright 2002 - 2019 Webdetails, a Hitachi Vantara company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package pt.webdetails.cda.utils.mondrian;


import mondrian.olap.Axis;
import mondrian.olap.Cell;
import mondrian.olap.Dimension;
import mondrian.olap.Member;
import mondrian.olap.Position;
import mondrian.olap.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.MetaAttributeNames;
import org.pentaho.reporting.engine.classic.core.MetaTableModel;
import org.pentaho.reporting.engine.classic.core.util.CloseableTableModel;
import org.pentaho.reporting.engine.classic.core.util.IntList;
import org.pentaho.reporting.engine.classic.core.wizard.DataAttributes;
import org.pentaho.reporting.engine.classic.core.wizard.DefaultConceptQueryMapper;
import org.pentaho.reporting.engine.classic.core.wizard.DefaultDataAttributes;
import org.pentaho.reporting.engine.classic.core.wizard.EmptyDataAttributes;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MDXMetaDataCellAttributes;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MDXMetaDataMemberAttributes;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.util.ResultSetProcessingLib;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ExtDenormalizedMDXTableModel extends AbstractTableModel implements CloseableTableModel, MetaTableModel {

  private static final Log logger = LogFactory.getLog( ExtDenormalizedMDXTableModel.class );

  private int[] axesSize;
  private IntList columnToAxisPosition;
  private int columnCount;
  private List<Member> columnToMemberMapping;
  private List<String> columnNames;
  private Result resultSet;
  private int rowCount;

  public ExtDenormalizedMDXTableModel( final Result resultSet,
                                       final int rowLimit,
                                       final boolean membersOnAxisSorted ) {
    ArgumentNullException.validate( "resultSet", resultSet );

    this.resultSet = resultSet;

    final Axis[] axes = this.resultSet.getAxes();

    this.axesSize = ResultSetProcessingLib.computeItemsPerAxis( axes );
    this.rowCount = computeRowCount( axesSize );
    final int[] axesMembers = ResultSetProcessingLib.computeTotalColumnsPerAxis( axes, 0, membersOnAxisSorted );

    this.columnCount = computeColumnCount( axesMembers );
    this.columnToAxisPosition = ResultSetProcessingLib.computeColumnToAxisMapping( axes, axesMembers, columnCount, 0 );
    this.columnToMemberMapping = Collections.unmodifiableList(
      ResultSetProcessingLib.computeColumnToMemberMapping( axes, axesMembers, 0, membersOnAxisSorted ) );
    this.columnNames = computeColumnNames( columnToMemberMapping );

    if ( rowLimit > 0 ) {
      rowCount = Math.min( rowLimit, rowCount );
    }
  }

  private List<String> computeColumnNames( final List<Member> columnToMemberMapper ) {
    ArrayList<String> columnNames = new ArrayList<String>();
    for ( final Member member : columnToMemberMapper ) {
      columnNames.add( member.getLevel().getUniqueName() );
    }

    final Member measureName = computeMeasureName( resultSet );
    if ( measureName != null ) {
      columnNames.add( MDXTableModelUtils.getProperMemberName( measureName, measureName.getUniqueName() ) );
      //columnNames.add(measureName.getUniqueName());
    } else {
      columnNames.add( "Measure" );
    }
    return columnNames;
  }

  private int computeColumnCount( final int[] axesMembers ) {
    if ( axesMembers.length == 0 ) {
      return 1;
    }

    boolean emptyAxisFound = false;
    int columnCount = 0;
    for ( int i = 0; i < axesMembers.length; i++ ) {
      columnCount += axesMembers[ i ];
      if ( axesMembers[ i ] == 0 ) {
        emptyAxisFound = true;
      }
    }

    if ( !emptyAxisFound ) {
      columnCount += 1;
    }

    return columnCount;
  }

  private int computeRowCount( final int[] axesSize ) {
    int rowCount = 1;
    for ( int i = 0; i < axesSize.length; i++ ) {
      final int size = axesSize[ i ];
      rowCount *= size;
    }

    return Math.max( 1, rowCount );
  }

  private Member computeMeasureName( final Result resultSet ) {
    final List<Position> positionList = resultSet.getSlicerAxis().getPositions();
    for ( int i = 0; i < positionList.size(); i++ ) {
      final Position position = positionList.get( i );
      for ( int positionIndex = 0; positionIndex < position.size(); positionIndex++ ) {

        Member m = position.get( positionIndex );
        while ( m != null ) {
          if ( m.isMeasure() ) {
            return m;
          }
          m = m.getParentMember();
        }
      }
    }

    return null;
  }

  public int getRowCount() {
    return rowCount;
  }

  public int getColumnCount() {
    return columnCount;
  }

  /**
   * Returns a default name for the column using spreadsheet conventions: A, B, C, ... Z, AA, AB, etc.  If
   * <code>column</code> cannot be found, returns an empty string.
   *
   * @param column the column being queried
   * @return a string containing the default name of <code>column</code>
   */
  public String getColumnName( final int column ) {
    return columnNames.get( column );
  }

  /**
   * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
   *
   * @param columnIndex the column being queried
   * @return the Object.class
   */
  public Class<?> getColumnClass( final int columnIndex ) {
    if ( getRowCount() == 0 ) {
      return Object.class;
    }
    try {
      final Object targetClassObj = getValueAt( 0, columnIndex );
      if ( targetClassObj == null ) {
        return Object.class;
      } else {
        return targetClassObj.getClass();
      }
    } catch ( final Exception e ) {
      return Object.class;
    }
  }

  public Object getValueAt( final int rowIndex,
                            final int columnIndex ) {
    if ( columnIndex >= columnNames.size() ) {
      throw new IndexOutOfBoundsException();
    }

    if ( isMeasureColumn( columnIndex ) ) {
      final int[] cellKey = computeCellKey( rowIndex );
      try {
        final Cell cell = resultSet.getCell( cellKey );
        if ( cell.isNull() ) {
          return null;
        }
        return cell.getValue();
      } catch ( NullPointerException pe ) {
        logger.error( "Error while getting value from table cell", pe );
      }
    }

    final int[] cellKey = computeCellKey( rowIndex );
    Member candidateMember = getCandidateMembers( columnIndex, cellKey );
    if ( candidateMember != null ) {
      return MDXTableModelUtils.getProperMemberName( candidateMember, candidateMember.getName() );
      //return candidateMember.getName();
    }
    return null;
  }

  private Member getCandidateMembers( final int columnIndex,
                                      final int[] cellKey ) {

    final int axisIndex = columnToAxisPosition.get( columnIndex );
    final Axis[] axes = resultSet.getAxes();
    final Axis axis = axes[ axisIndex ];

    final List<Position> positionList = axis.getPositions();
    if ( positionList.isEmpty() ) {
      return null;
    }

    final int posIndex = cellKey[ axisIndex ];
    final Position position = positionList.get( posIndex );

    final Member memberByName = findMemberByName( position, columnIndex );
    if ( memberByName != null ) {
      return memberByName;
    }
    return findRootMember( position, columnIndex );
  }

  private Member findRootMember( final List<Member> position, final int columnIndex ) {
    final Dimension dimension = columnToMemberMapping.get( columnIndex ).getDimension();
    for ( int i = 0; i < position.size(); i++ ) {
      final Member member = position.get( i );
      if ( dimension.equals( member.getDimension() ) ) {
        if ( member.getParentMember() == null ) {
          return member;
        }
      }
    }
    return null;
  }

  private Member findMemberByName( final List<Member> position, final int columnIndex ) {
    final Dimension dimension = columnToMemberMapping.get( columnIndex ).getDimension();
    for ( int i = 0; i < position.size(); i++ ) {
      final Member member = position.get( i );
      if ( dimension.equals( member.getDimension() ) ) {
        Member match = searchContextMemberOfParents( member, columnIndex );
        if ( match != null ) {
          return match;
        }
      }
    }
    return null;
  }

  private Member searchContextMemberOfParents( final Member member, final int columnIndex ) {
    String columnName = getColumnName( columnIndex );

    Member candidate = member;
    while ( candidate != null ) {
      if ( candidate.getLevel().getUniqueName().equals( columnName ) ) {
        return candidate;
      }
      candidate = candidate.getParentMember();
    }
    return null;
  }

  public void close() {
    resultSet.close();
  }

  /**
   * Returns the meta-attribute as Java-Object. The object type that is expected by the report engine is defined in the
   * TableMetaData property set. It is the responsibility of the implementor to map the native meta-data model into a
   * model suitable for reporting.
   * <p/>
   * Meta-data models that only describe meta-data for columns can ignore the row-parameter.
   *
   * @param rowIndex    the row of the cell for which the meta-data is queried.
   * @param columnIndex the index of the column for which the meta-data is queried.
   * @return the meta-data object.
   */
  public DataAttributes getCellDataAttributes( final int rowIndex, final int columnIndex ) {
    if ( columnIndex >= columnNames.size() ) {
      throw new IndexOutOfBoundsException();
    }

    if ( isMeasureColumn( columnIndex ) ) {
      final int[] cellKey = computeCellKey( rowIndex );
      final Cell cell = resultSet.getCell( cellKey );
      return new MDXMetaDataCellAttributes( EmptyDataAttributes.INSTANCE, cell );
    }

    final int[] cellKey = computeCellKey( rowIndex );
    Member contextMember = getCandidateMembers( columnIndex, cellKey );
    if ( contextMember != null ) {
      return new MDXMetaDataMemberAttributes( EmptyDataAttributes.INSTANCE, contextMember );
    }
    return EmptyDataAttributes.INSTANCE;
  }

  private boolean isMeasureColumn( final int columnIndex ) {
    if ( columnIndex >= columnToMemberMapping.size() ) {
      return true;
    }
    return false;
  }

  private int[] computeCellKey( final int rowIndex ) {
    final int[] cellKey = new int[ axesSize.length ];
    int tmpRowIdx = rowIndex;
    for ( int i = 0; i < axesSize.length; i++ ) {
      final int axisSize = axesSize[ i ];
      if ( axisSize == 0 ) {
        cellKey[ i ] = 0;
      } else {
        final int pos = tmpRowIdx % axisSize;
        cellKey[ i ] = pos;
        tmpRowIdx = tmpRowIdx / axisSize;
      }
    }

    return cellKey;
  }

  public boolean isCellDataAttributesSupported() {
    return true;
  }

  public DataAttributes getColumnAttributes( final int column ) {
    return EmptyDataAttributes.INSTANCE;
  }

  /**
   * Returns table-wide attributes. This usually contain hints about the data-source used to query the data as well as
   * hints on the sort-order of the data.
   *
   * @return
   */
  public DataAttributes getTableAttributes() {
    final DefaultDataAttributes dataAttributes = new DefaultDataAttributes();
    dataAttributes.setMetaAttribute( MetaAttributeNames.Core.NAMESPACE,
      MetaAttributeNames.Core.CROSSTAB_MODE, DefaultConceptQueryMapper.INSTANCE,
      MetaAttributeNames.Core.CROSSTAB_VALUE_NORMALIZED );
    return dataAttributes;
  }

}
