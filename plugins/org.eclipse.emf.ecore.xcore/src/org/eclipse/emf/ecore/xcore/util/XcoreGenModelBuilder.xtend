/**
 * Copyright (c) 2011-2012 Eclipse contributors and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.emf.ecore.xcore.util

import com.google.inject.Inject
import java.util.Collections
import java.util.HashSet
import org.eclipse.emf.codegen.ecore.genmodel.GenClass
import org.eclipse.emf.codegen.ecore.genmodel.GenDataType
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature
import org.eclipse.emf.codegen.ecore.genmodel.GenModel
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory
import org.eclipse.emf.codegen.ecore.genmodel.GenOperation
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.xcore.XClass
import org.eclipse.emf.ecore.xcore.XDataType
import org.eclipse.emf.ecore.xcore.XOperation
import org.eclipse.emf.ecore.xcore.XPackage
import org.eclipse.emf.ecore.xcore.XStructuralFeature
import org.eclipse.emf.ecore.xcore.mappings.XcoreMapper

import static extension org.eclipse.emf.ecore.xcore.XcoreExtensions.*
import org.eclipse.emf.codegen.ecore.genmodel.GenEnumLiteral
import org.eclipse.emf.ecore.xcore.XEnumLiteral
import org.eclipse.emf.common.util.UniqueEList
import java.util.List
import org.eclipse.emf.codegen.ecore.genmodel.GenParameter
import org.eclipse.emf.ecore.xcore.XParameter
import org.eclipse.emf.codegen.ecore.genmodel.GenTypeParameter
import org.eclipse.emf.ecore.xcore.XTypeParameter

class XcoreGenModelBuilder
{
  @Inject extension XcoreMapper mapper

  @Inject XcoreGenModelInitializer genModelInitializer

  def GenModel getGenModel(XPackage pack)
  {
    val ePackage = pack.mapping.getEPackage
    val genModel =  GenModelFactory::eINSTANCE.createGenModel()
    genModel.initialize(Collections::singleton(ePackage))
    pack.eResource.getContents().add(1, genModel)
    buildMap(genModel)
    return genModel
  }

  def buildMap(GenModel genModel)
  {
    for (genElement : genModel.allContentsIterable)
    {
      switch genElement
      {
        GenPackage:
        {
          val xPackage = genElement.getEcorePackage.toXcoreMapping.xcoreElement as XPackage
          if (xPackage != null)
          {
            xPackage.mapping.genPackage = genElement
            genElement.toXcoreMapping.xcoreElement = xPackage
          }
        }
        GenClass:
        {
          val xClass = genElement.ecoreClass.toXcoreMapping.xcoreElement as XClass
          if (xClass != null)
          {
            xClass.mapping.genClass = genElement
            genElement.toXcoreMapping.xcoreElement = xClass
          }
        }
        GenDataType:
        {
          val xDataType = genElement.ecoreDataType.toXcoreMapping.xcoreElement as XDataType
          if (xDataType != null)
          {
            xDataType.mapping.genDataType = genElement
            genElement.toXcoreMapping.xcoreElement = xDataType
          }
        }
        GenFeature:
        {
          val xFeature = genElement.ecoreFeature.toXcoreMapping.xcoreElement as XStructuralFeature
          if (xFeature != null)
          {
            xFeature.mapping.genFeature = genElement
            genElement.toXcoreMapping.xcoreElement = xFeature
          }
        }
        GenOperation:
        {
          val xOperation = genElement.ecoreOperation.toXcoreMapping.xcoreElement as XOperation
          if (xOperation != null)
          {
            xOperation.mapping.genOperation = genElement
            genElement.toXcoreMapping.xcoreElement = xOperation
          }
        }
        GenParameter:
        {
          val xParameter = genElement.ecoreParameter.toXcoreMapping.xcoreElement as XParameter
          if (xParameter != null)
          {
            xParameter.mapping.genParameter = genElement
            genElement.toXcoreMapping.xcoreElement = xParameter
          }
        }
        GenTypeParameter:
        {
          val xTypeParameter = genElement.ecoreTypeParameter.toXcoreMapping.xcoreElement as XTypeParameter
          if (xTypeParameter != null)
          {
            xTypeParameter.mapping.genTypeParameter = genElement
            genElement.toXcoreMapping.xcoreElement = xTypeParameter
          }
        }
        GenEnumLiteral:
        {
          val xEnumLiteral = genElement.ecoreEnumLiteral.toXcoreMapping.xcoreElement as XEnumLiteral
          if (xEnumLiteral != null)
          {
            xEnumLiteral.mapping.genEnumLiteral = genElement
            genElement.toXcoreMapping.xcoreElement = xEnumLiteral
          }
        }
      }
    }
  }

  def initializeUsedGenPackages(GenModel genModel)
  {
    genModelInitializer.initialize(genModel, true)
    val referencedEPackages = new HashSet<EPackage>()
    val List<EPackage> ePackages = new UniqueEList<EPackage>()
    for (genPackage : genModel.genPackages)
    {
      ePackages.add(genPackage.getEcorePackage)
    }

    var int i = 0
    while (i < ePackages.size())
    {
      val ePackage = ePackages.get(i)
      i = i + 1
      val allContents = ePackage.eAllContents
      while (allContents.hasNext())
      {
        val eObject = allContents.next()
        if (eObject instanceof EPackage)
        {
          allContents.prune()
        }
        else
        {
         for (eCrossReference : eObject.eCrossReferences)
         {
           switch eCrossReference
           {
             EClassifier:
             {
               val EPackage referencedEPackage = eCrossReference.getEPackage
               ePackages.add(referencedEPackage)
               referencedEPackages.add(referencedEPackage)
             }
             EStructuralFeature:
             {
               val EPackage referencedEPackage = eCrossReference.getEContainingClass().getEPackage
               ePackages.add(referencedEPackage)
               referencedEPackages.add(referencedEPackage)
             }
           }
         }
       }
      }
    }

    for (referencedEPackage : referencedEPackages)
    {
      if (genModel.findGenPackage(referencedEPackage) == null)
      {
        var usedGenPackage = mapper.getGen(mapper.getToXcoreMapping(referencedEPackage).xcoreElement) as GenPackage
        if (usedGenPackage == null)
        {
          usedGenPackage = findLocalGenPackage(referencedEPackage)
        }
        if (usedGenPackage != null)
        {
          genModel.usedGenPackages.add(usedGenPackage)
        }
        else
        {
          val resources = genModel.eResource.resourceSet.resources
          i = 0
          var boolean found = false
          while (i < resources.size && !found)
          {
            val resource = resources.get(i)
            if ("genmodel".equals(resource.URI.fileExtension) && !resource.getContents().isEmpty())
            {
              usedGenPackage = (resource.contents.get(0) as GenModel).findGenPackage(referencedEPackage)
              if (usedGenPackage != null)
              {
                 genModel.usedGenPackages.add(usedGenPackage)
                 found = true
              }
            }
            i = i + 1
          }
          if (!found)
          {
            throw new RuntimeException("No GenPackage found for " + referencedEPackage)
          }
        }
      }
    }
  }

  def findLocalGenPackage(EPackage ePackage)
  {
    if (ePackage.eResource != null)
    {
      for (content : ePackage.eResource.contents)
      {
        if (content instanceof GenModel)
        {
          val genPackage = (content as GenModel).findGenPackage(ePackage)
          if (genPackage != null)
          {
           return genPackage
          }
        }
      }
    }
  }
}