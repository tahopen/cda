/*!
 * Copyright 2002 - 2017 Webdetails, a Hitachi Vantara company. All rights reserved.
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.table.AbstractTableModel;

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
import org.pentaho.reporting.engine.classic.core.wizard.DataAttributes;
import org.pentaho.reporting.engine.classic.core.wizard.DefaultConceptQueryMapper;
import org.pentaho.reporting.engine.classic.core.wizard.DefaultDataAttributes;
import org.pentaho.reporting.engine.classic.core.wizard.EmptyDataAttributes;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MDXMetaDataCellAttributes;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MDXMetaDataMemberAttributes;

/**
 * This tablemodel performs some preprocessing to get multi-dimensional resultset (with row and column headers) into a
 * classical table-structure. The query must be a two-dimensional query or the whole process will break.
 * <p/>
 * This class exists for legacy reasons to provide existing reports the same view on MDX data as implemented in the
 * Pentaho-Platform and the Report-Designer. It can also be somewhat useful if you have a requirement to produce banded
 * reporting over a MDX data source.
 */

public class CompactBandedMDXTableModel extends AbstractTableModel
  implements CloseableTableModel, MetaTableModel {

  private static final long serialVersionUID = 1L;

  private static final Log logger = LogFactory.getLog( CompactBandedMDXTableModel.class );
  private boolean noMeasures;
  private Result resultSet;
  private int rowCount;
  private int columnCount;
  private String[] columnNames;
  private int[] axesSize;
  private int[] columnToAxisPosition;
  private Dimension[] columnToDimensionMapping;

  public CompactBandedMDXTableModel( final Result resultSet, final int rowLimit ) {
    if ( resultSet == null ) {
      throw new NullPointerException( "ResultSet returned was null" );
    }
    this.resultSet = resultSet;

    // rowcount is the product of all axis-sizes. If an axis contains more than one member, then
    // Mondrian already performs the crossjoin for us.

    // column count is the count of all hierachies of all axis.

    final Axis[] axes = this.resultSet.getAxes();
    this.rowCount = 0;
    this.axesSize = new int[ axes.length ];
    final int[] axesMembers = new int[ axes.length ];
    @SuppressWarnings( "unchecked" )
    final List<Dimension>[] dimensionsForMembersPerAxis = new List[ axes.length ];
    @SuppressWarnings( "unchecked" )
    final List<Integer>[] membersPerAxis = new List[ axes.length ];

    // process the column axis first ..
    if ( axesSize.length > 0 ) {
      final Axis axis = axes[ 0 ];
      final List<Position> positions = axis.getPositions();

      axesSize[ 0 ] = positions.size();
      if ( positions.isEmpty() ) {
        noMeasures = true;
      }
    }

    // Axis contains (zero or more) positions, which contains (zero or more) members
    for ( int axesIndex = axes.length - 1; axesIndex >= 1; axesIndex -= 1 ) {
      final Axis axis = axes[ axesIndex ];
      final List<Position> positions = axis.getPositions();

      axesSize[ axesIndex ] = positions.size();
      if ( positions.isEmpty() ) {
        noMeasures = true;
      }

      final ArrayList<Integer> memberList = new ArrayList<Integer>();
      final ArrayList<Dimension> dimensionsForMembers = new ArrayList<Dimension>();
      for ( int positionsIndex = 0; positionsIndex < positions.size(); positionsIndex++ ) {
        final Position position = positions.get( positionsIndex );
        for ( int positionIndex = 0; positionIndex < position.size(); positionIndex++ ) {
          Member m = position.get( positionIndex );
          final Dimension dimension = m.getDimension();
          int hierarchyLevelCount = 1; // Originally was 0

          //          // Change compared to BandedMDXTM - we don't want all levels
          //          while (false && m != null)
          //          {
          //            m = m.getParentMember();
          //            hierarchyLevelCount += 1;
          //          }

          if ( memberList.size() <= positionIndex ) {
            memberList.add( hierarchyLevelCount );
            dimensionsForMembers.add( dimension );
          } else {
            final Integer existingLevel = memberList.get( positionIndex );
            if ( existingLevel.intValue() < hierarchyLevelCount ) {
              memberList.set( positionIndex, hierarchyLevelCount );
              dimensionsForMembers.set( positionIndex, dimension );
            }
          }
        }
      }

      int memberCount = 0;
      for ( int i = 0; i < memberList.size(); i++ ) {
        memberCount += memberList.get( i );
      }
      axesMembers[ axesIndex ] = memberCount;
      dimensionsForMembersPerAxis[ axesIndex ] = dimensionsForMembers;
      membersPerAxis[ axesIndex ] = memberList;
    }

    if ( axesSize.length > 1 ) {
      rowCount = axesSize[ 1 ];
      for ( int i = 2; i < axesSize.length; i++ ) {
        final int size = axesSize[ i ];
        rowCount *= size;
      }
    }
    if ( noMeasures == false ) {
      rowCount = Math.max( 1, rowCount );
    }
    if ( axesSize.length == 0 ) {
      columnCount = 1;
    } else if ( axesSize.length > 0 ) {
      columnCount = axesSize[ 0 ];
    }
    for ( int i = 1; i < axesMembers.length; i++ ) {
      columnCount += axesMembers[ i ];
    }

    columnNames = new String[ columnCount ];
    columnToDimensionMapping = new Dimension[ columnCount ];
    columnToAxisPosition = new int[ columnCount ];

    int columnIndex = 0;
    int dimColIndex = 0;

    //    final FastStack memberStack = new FastStack();
    for ( int axesIndex = axes.length - 1; axesIndex >= 1; axesIndex -= 1 ) {
      final Axis axis = axes[ axesIndex ];
      final List<Position> positions = axis.getPositions();
      final LinkedHashSet<String> columnNamesSet = new LinkedHashSet<String>();
      for ( int positionsIndex = 0; positionsIndex < positions.size(); positionsIndex++ ) {
        final Position position = positions.get( positionsIndex );
        for ( int positionIndex = 0; positionIndex < position.size(); positionIndex++ ) {
          //          memberStack.clear();
          Member m = position.get( positionIndex );
          // Get member's hierarchy
          final String name = m.getHierarchy().getName();
          if ( columnNamesSet.contains( name ) == false ) {
            columnNamesSet.add( name );
          }

        }
      }

      if ( columnNamesSet.size() != axesMembers[ axesIndex ] ) {
        logger.error( "ERROR: Number of names is not equal the pre-counted number." );
      }

      final List<Dimension> dimForMemberPerAxis = dimensionsForMembersPerAxis[ axesIndex ];
      final List<Integer> memberCntPerAxis = membersPerAxis[ axesIndex ];
      for ( int i = 0; i < memberCntPerAxis.size(); i++ ) {
        final Integer count = memberCntPerAxis.get( i );
        final Dimension dim = dimForMemberPerAxis.get( i );
        for ( int x = 0; x < count.intValue(); x += 1 ) {
          this.columnToDimensionMapping[ dimColIndex + x ] = dim;
          this.columnToAxisPosition[ dimColIndex + x ] = axesIndex;
        }
        dimColIndex = count.intValue() + dimColIndex;
      }

      final String[] names = columnNamesSet.toArray( new String[ columnNamesSet.size() ] );
      System.arraycopy( names, 0, this.columnNames, columnIndex, names.length );
      columnIndex += names.length;
    }

    if ( axesSize.length > 0 ) {
      // now create the column names for the column-axis
      final Axis axis = axes[ 0 ];
      final List<Position> positions = axis.getPositions();
      for ( int i = 0; i < positions.size(); i++ ) {
        final Position position = positions.get( i );
        final StringBuffer positionName = new StringBuffer( 100 );
        for ( int j = 0; j < position.size(); j++ ) {
          if ( j != 0 ) {
            positionName.append( '/' );
          }
          final Member member = position.get( j );
          positionName.append( MDXTableModelUtils.getProperMemberName( member, member.getName() ) );

        }
        columnNames[ columnIndex ] = positionName.toString();
        columnIndex += 1;
      }
    }
    if ( axesSize.length == 0 ) {
      columnNames[ 0 ] = "Measure";
    }
    if ( rowLimit > 0 ) {
      rowCount = Math.min( rowLimit, rowCount );
    }
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
    return columnNames[ column ];
  }

  public Object getValueAt( final int rowIndex,
                            final int columnIndex ) {
    if ( columnIndex >= columnNames.length ) {
      throw new IndexOutOfBoundsException();
    }

    final int correctedColIndex;
    if ( axesSize.length > 0 ) {
      final int startOfColumnIndex = columnCount - axesSize[ 0 ];
      if ( columnIndex < startOfColumnIndex ) {
        // this is a query for a axis-header
        correctedColIndex = -1;
      } else {
        correctedColIndex = columnIndex - startOfColumnIndex;
      }
    } else {
      correctedColIndex = 0;
    }

    final int[] cellKey = computeCellKey( rowIndex, correctedColIndex );

    // user asked for a dimension ...
    final Dimension dimension = columnToDimensionMapping[ columnIndex ];
    if ( dimension == null ) {
      final Cell cell = resultSet.getCell( cellKey );
      if ( cell.isNull() ) {
        return null;
      }
      return cell.getValue();
    }

    Member contextMember = getContextMember( dimension, columnIndex, cellKey );
    if ( contextMember != null ) {
      return contextMember.getName();
    }

    return null;
  }

  /**
   * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
   *
   * @param columnIndex the column being queried
   * @return the Object.class
   */
  public Class<?> getColumnClass( int columnIndex ) {
    try {
      Object targetClassObj = getValueAt( 0, columnIndex );


      return targetClassObj == null ? Object.class : targetClassObj.getClass();
    } catch ( Exception e ) {
      return Object.class;
    }


  }

  private int[] computeCellKey( final int rowIndex, final int columnIndex ) {
    final int[] cellKey = new int[ axesSize.length ];


    int tmpRowIdx = rowIndex;


    if ( axesSize.length > 0 ) {
      cellKey[ 0 ] = columnIndex;


    }
    for ( int i = 1; i
      < axesSize.length; i++ ) {
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

  private Member getContextMember( final Dimension dimension,
                                   final int columnIndex,
                                   final int[] cellKey ) {
    final int axisIndex = columnToAxisPosition[ columnIndex ];
    final Axis[] axes = resultSet.getAxes();
    final Axis axis = axes[ axisIndex ];
    final int posIndex = cellKey[ axisIndex ];
    final List<Position> positionList = axis.getPositions();


    if ( positionList.isEmpty() ) {
      return null;


    }

    final Position position = positionList.get( posIndex );


    for ( int i = 0; i
      < position.size(); i++ ) {
      final Member member = position.get( i );


      if ( dimension.equals( member.getDimension() ) ) {
        return member;


      }
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
    if ( columnIndex >= columnNames.length ) {
      throw new IndexOutOfBoundsException();


    }

    final int[] cellKey = computeCellKey( rowIndex, columnIndex );

    // user asked for a dimension ...
    final Dimension dimension = columnToDimensionMapping[ columnIndex ];


    if ( dimension == null ) {
      final Cell cell = resultSet.getCell( cellKey );


      return new MDXMetaDataCellAttributes( EmptyDataAttributes.INSTANCE, cell );


    }

    Member contextMember = getContextMember( dimension, columnIndex, cellKey );


    while ( contextMember != null ) {
      if ( contextMember.getLevel().getUniqueName().equals( getColumnName( columnIndex ) ) ) {
        return new MDXMetaDataMemberAttributes( EmptyDataAttributes.INSTANCE, contextMember );


      }
      contextMember = contextMember.getParentMember();


    }

    return EmptyDataAttributes.INSTANCE;


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
   * @return the table attributes.
   */
  public DataAttributes getTableAttributes() {
    final DefaultDataAttributes dataAttributes = new DefaultDataAttributes();
    dataAttributes.setMetaAttribute( MetaAttributeNames.Core.NAMESPACE,
      MetaAttributeNames.Core.CROSSTAB_MODE, DefaultConceptQueryMapper.INSTANCE,
      MetaAttributeNames.Core.CROSSTAB_VALUE_NORMALIZED );


    return dataAttributes;

  }

}
